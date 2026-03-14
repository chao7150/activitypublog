package com.example.log.infrastructure.api.web

import com.example.log.domain.gateway.PostRepository
import com.example.log.domain.model.ActivityPost
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/")
class TimelineController(
    private val postRepository: PostRepository
) {

    /**
     * タイムラインのメインページ
     */
    @GetMapping
    fun index(model: Model): String {
        val posts = postRepository.findAllOrderByPublishedDesc().take(20)
        model.addAttribute("posts", posts)
        return "timeline/index"
    }

    /**
     * 追加の投稿を読み込む（HTMX用フラグメント）
     */
    @GetMapping("/posts/more")
    fun loadMore(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        model: Model
    ): String {
        // 本来は Repository に Pageable なメソッドを追加すべきだが、一旦全取得から take
        val posts = postRepository.findAllOrderByPublishedDesc()
            .drop((page + 1) * size)
            .take(size)
        
        model.addAttribute("posts", posts)
        model.addAttribute("nextPage", page + 1)
        return "timeline/fragments/post-list :: post-list"
    }
}
