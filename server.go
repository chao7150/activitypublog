package activitypublog

import (
	"context"
	"database/sql"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"net/url"
	"os"
	"text/template"
	"time"

	"github.com/go-sql-driver/mysql"
	"github.com/joho/godotenv"
	"github.com/labstack/echo/v4"
	"github.com/labstack/echo/v4/middleware"
	"github.com/uptrace/bun"
	"github.com/uptrace/bun/dialect/mysqldialect"
)

var db *sql.DB
var bundb *bun.DB
var ctx = context.Background()

type PostOauthTokenResponse struct {
	AccessToken  string `json:"access_token"`
	TokenType    string `json:"token_type"`
	Scope        string `json:"scope"`
	CreatedAt    int64  `json:"created_at"`
	RefreshToken string `json:"refresh_token"`
}

func StartServer() {
	err := godotenv.Load(".env")
	if err != nil {
		fmt.Println("failed to load env file")
		return
	}

	cfg := mysql.Config{
		User:      os.Getenv("MYSQL_USER"),
		Passwd:    os.Getenv("MYSQL_PASSWORD"),
		Net:       "tcp",
		Addr:      os.Getenv("MYSQL_HOST") + ":3306",
		DBName:    os.Getenv("MYSQL_DATABASE"),
		ParseTime: true,
	}

	db, err = sql.Open("mysql", cfg.FormatDSN())
	if err != nil {
		log.Fatal(err)
	}
	pingErr := db.Ping()
	if pingErr != nil {
		log.Fatal(pingErr)
	}
	fmt.Println("datebase connection established.")
	bundb = bun.NewDB(db, mysqldialect.New())

	var errors []error
	if _, err = bundb.NewCreateTable().Model((*App)(nil)).IfNotExists().Exec(ctx); err != nil {
		errors = append(errors, err)
	}
	if _, err = bundb.NewCreateTable().Model((*Account)(nil)).IfNotExists().Exec(ctx); err != nil {
		errors = append(errors, err)
	}
	if _, err = bundb.NewCreateTable().Model((*Status)(nil)).ForeignKey("(`account_id`, `host`) REFERENCES account (`id`, `host`) ON DELETE CASCADE").ForeignKey("(`visibility`) REFERENCES visibility (`visibility`) ON DELETE CASCADE ON UPDATE CASCADE").IfNotExists().Exec(ctx); err != nil {
		errors = append(errors, err)
	}
	if 0 < len(errors) {
		fmt.Printf("failed to initialize db table: %v", errors)
	}

	t := &Template{
		templates: template.Must(template.ParseGlob("public/views/*.html")),
	}

	e := echo.New()
	e.Use(middleware.Gzip())
	e.Renderer = t
	e.Static("/static", "assets")
	e.GET("/", func(c echo.Context) error {
		SendAndOutputError := HandlerError("GET", "/", c)
		token, host, err := RequireLoggedIn(c)
		if err != nil {
			return err
		}
		account, err := hGetVerifyCredentials(host, token)
		if err != nil {
			return SendAndOutputError(err)
		}
		account, err = dSelectAccount(account.Id, host)
		if err != nil {
			return SendAndOutputError(err)
		}
		query := c.QueryParam("q")
		allStatuses, err := dSelectStatusesByAccountAndText(account.Id, query)
		if err != nil {
			return SendAndOutputError(err)
		}
		noMoreNewerStatuses := c.QueryParam("noMoreNewerStatuses") == "true"
		props := TopProps{Account: account, Statuses: allStatuses, AllFetched: account.AllFetched, NoMoreNewerStatuses: noMoreNewerStatuses, Public: account.Public}

		return c.Render(http.StatusOK, "top", props)
	})
	e.POST("/status/cursor/head", func(c echo.Context) error {
		SendAndOutputError := HandlerError("POST", "/status/cursor/head", c)
		token, host, err := RequireLoggedIn(c)
		if err != nil {
			return err
		}
		account, nil := hGetVerifyCredentials(host, token)
		if err != nil {
			return SendAndOutputError(err)
		}
		newestStatusId, err := dSelectNewestStatusIdByAccount(account.Id)
		if err != nil {
			return SendAndOutputError(err)
		}
		newStatuses, nil := hGetAccountStatusesAll(host, token, account.Id, newestStatusId, "")
		if err != nil {
			return SendAndOutputError(err)
		}
		if len(newStatuses) == 0 {
			return c.Redirect(302, "/?noMoreNewerStatuses=true")
		}
		_, err = dInsertStatuses(newStatuses, account.Id, host)
		if err != nil {
			fmt.Printf("db insert error: %v", err)
		}
		return c.Redirect(302, "/")
	})
	e.POST("/status/cursor/last", func(c echo.Context) error {
		SendAndOutputError := HandlerError("POST", "/status/cursor/last", c)
		token, host, err := RequireLoggedIn(c)
		if err != nil {
			return err
		}
		account, nil := hGetVerifyCredentials(host, token)
		if err != nil {
			return SendAndOutputError(err)
		}
		allFetched, err := dSelectAccountAllFetchedById(account.Id, host)
		if err != nil {
			return SendAndOutputError(err)
		}
		if allFetched {
			return c.Redirect(302, "/?allFetched=true")
		}
		for {
			oldestStatusId, err := dSelectOldestStatusIdByAccount(account.Id)
			if err != nil {
				return SendAndOutputError(err)
			}
			newStatuses, nil := hGetAccountStatusesOlderThan(host, token, account.Id, oldestStatusId)
			if err != nil {
				return SendAndOutputError(err)
			}
			if len(newStatuses) == 0 {
				if err := dUpdateAccountAllFetched(account.Id); err != nil {
					return SendAndOutputError(err)
				}
				return c.Redirect(302, "/")
			}
			_, err = dInsertStatuses(newStatuses, account.Id, host)
			if err != nil {
				fmt.Printf("db insert error: %v", err)
			}
			time.Sleep(time.Second * 2)
		}
	})
	e.File("/login", "static/login.html")
	e.GET("/logout", func(c echo.Context) error {
		tokenCookie := &http.Cookie{
			Name:    "token",
			Value:   "",
			Expires: time.Unix(0, 0),
		}
		c.SetCookie(tokenCookie)
		hostCookie := &http.Cookie{
			Name:    "host",
			Value:   "",
			Expires: time.Unix(0, 0),
		}
		c.SetCookie(hostCookie)
		return c.Redirect(302, "/login")
	})
	e.POST("/sign_in", func(c echo.Context) error {
		SendAndOutputError := HandlerError("POST", "/sign_in", c)
		host := c.FormValue("host")
		app, err := dSelectAppByHost(host)
		if err != nil {
			fmt.Println("app data was not found in db. fetch it.")
			app, err = hPostApp(host, os.Getenv("BASE_URL"))
			if err != nil {
				return SendAndOutputError(err)
			}
			err = dInsertApp(app)
			if err != nil {
				return SendAndOutputError(err)
			}
		}
		u := url.URL{}
		u.Scheme = "https"
		u.Host = host
		u.Path = "/oauth/authorize"
		q := url.Values{"response_type": {"code"}, "client_id": {app.ClientId}, "redirect_uri": {os.Getenv("BASE_URL") + "/authorize"}}
		u.RawQuery = q.Encode()
		cookie := &http.Cookie{
			Name:    "authentication-ongoing-instance-name",
			Value:   host,
			Expires: time.Now().Add(5 * time.Minute),
			Path:    "/authorize",
		}
		c.SetCookie(cookie)
		return c.Redirect(302, u.String())
	})
	e.GET("/authorize", func(c echo.Context) error {
		SendAndOutputError := HandlerError("GET", "/authorize", c)
		cookie, err := c.Cookie("authentication-ongoing-instance-name")
		if err != nil {
			return c.Redirect(302, "/")
		}
		host := cookie.Value
		code := c.QueryParam("code")
		u := url.URL{}
		u.Scheme = "https"
		u.Host = host
		u.Path = "/oauth/token"
		app, err := dSelectAppByHost(host)
		if err != nil {
			return SendAndOutputError(err)
		}
		q := url.Values{"grant_type": {"authorization_code"}, "code": {code}, "client_id": {app.ClientId}, "client_secret": {app.ClientSecret}, "redirect_uri": {os.Getenv("BASE_URL") + "/authorize"}}
		resp, err := http.PostForm(u.String(), q)
		if err != nil {
			return c.String(http.StatusBadRequest, fmt.Sprintf("failed to create app for the host: %v", err))
		}
		defer resp.Body.Close()
		body, err := io.ReadAll(resp.Body)
		if err != nil {
			return c.String(http.StatusInternalServerError, fmt.Sprintf("failed to read response from server: %v", err))
		}
		var r PostOauthTokenResponse
		if err := json.Unmarshal(body, &r); err != nil {
			return c.String(http.StatusInternalServerError, fmt.Sprintf("failed to parse response from server: %v", err))
		}
		account, err := hGetVerifyCredentials(host, r.AccessToken)
		if err != nil {
			return SendAndOutputError(err)
		}
		_, err = dInsertAccountIfNotExists(account.Id, account.UserName, host)
		if err != nil {
			return SendAndOutputError(err)
		}
		tokenCookie := &http.Cookie{
			Name:    "token",
			Value:   r.AccessToken,
			Expires: time.Now().Add(24 * 7 * time.Hour),
		}
		c.SetCookie(tokenCookie)
		hostCookie := &http.Cookie{
			Name:    "host",
			Value:   host,
			Expires: time.Now().Add(24 * 7 * time.Hour),
		}
		c.SetCookie(hostCookie)
		return c.Redirect(302, "/")
	})
	e.GET("/users/:host/:username", func(c echo.Context) error {
		SendAndOutputError := HandlerError("GET", "/users/:host/:username", c)
		username := c.Param("username")
		host := c.Param("host")
		account, err := dSelectAccountByUserName(username, host)
		if err != nil {
			return SendAndOutputError(err)
		}
		if !account.Public {
			return c.String(http.StatusNotFound, "not found")
		}
		statuses, err := dSelectStatusesByAccountWithRestriction(username, host)
		if err != nil {
			return SendAndOutputError(err)
		}

		props := UsersProps{Host: host, UserName: username, Statuses: statuses}

		return c.Render(http.StatusOK, "users", props)
	})
	e.POST("/status/public", func(c echo.Context) error {
		SendAndOutputError := HandlerError("POST", "/status/public", c)
		token, host, err := RequireLoggedIn(c)
		if err != nil {
			return err
		}
		public := c.FormValue("public") == "true"
		account, nil := hGetVerifyCredentials(host, token)
		if err != nil {
			return SendAndOutputError(err)
		}
		err = dUpdateAccountPublic(account.Id, host, public)
		if err != nil {
			return SendAndOutputError(err)
		}
		return c.Redirect(302, "/")
	})
	e.POST("/account/visibility", func(c echo.Context) error {
		SendAndOutputError := HandlerError("POST", "/account/visibility", c)
		token, host, err := RequireLoggedIn(c)
		if err != nil {
			return err
		}
		showUnlisted := c.FormValue("unlisted") == "on"
		showPrivate := c.FormValue("private") == "on"
		showDirect := c.FormValue("direct") == "on"
		account, nil := hGetVerifyCredentials(host, token)
		if err != nil {
			return SendAndOutputError(err)
		}
		err = dUpdateAccountVisibility(account.Id, host, showUnlisted, showPrivate, showDirect)
		if err != nil {
			return SendAndOutputError(err)
		}
		return c.Redirect(302, "/")
	})

	e.Logger.Fatal(e.Start(":1323"))
}
