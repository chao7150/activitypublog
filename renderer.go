package activitypublog

import (
	"io"
	"text/template"

	"github.com/labstack/echo/v4"
)

type Template struct {
	templates *template.Template
}

func (t *Template) Render(w io.Writer, name string, data interface{}, c echo.Context) error {
	return t.templates.ExecuteTemplate(w, name, data)
}

type TopProps struct {
	Account             Account
	Statuses            []Status
	AllFetched          bool
	NoMoreNewerStatuses bool
	Public              bool
}

type UsersProps struct {
	Host     string
	UserName string
	Statuses []Status
}
