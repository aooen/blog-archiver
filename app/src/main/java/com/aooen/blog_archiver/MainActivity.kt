package com.aooen.blog_archiver

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aooen.blog_archiver.database.*
import com.aooen.blog_archiver.databinding.ActivityMainBinding
import org.jsoup.Jsoup
import java.util.*

class MainActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val interfaceId by lazy {
        val charPool : List<Char> = ('a'..'z') + ('A'..'Z')
        (1..5).map { charPool[kotlin.random.Random.nextInt(0, charPool.size)] }.joinToString("")
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.webView.apply {
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)

                    supportActionBar?.hide()

                    CookieManager.getInstance().run {
                        setAcceptCookie(true)
                        setAcceptThirdPartyCookies(binding.webView, true)
                        flush()
                    }

                    if (!url.isNullOrEmpty() && url.indexOf("m.blog.naver.com/") >= 0) {
                        view?.loadUrl("javascript:window.$interfaceId.get(document.documentElement.innerHTML);")
                    }
                }
            }

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }

            addJavascriptInterface(JavaScriptInterface(::onSuccess, ::onError), interfaceId)
            loadUrl("https://m.blog.naver.com")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.listAction -> {
                startActivity(Intent(this, ListActivity::class.java))
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        supportActionBar?.run {
            if (isShowing) {
                binding.webView.goBack()
            } else {
                show()
            }
        }
    }

    private fun onSuccess(blog: Blog, article: Article) {
        SaveThread(this, blog, article).start()
    }

    private fun onError() {
        Toast.makeText(this, getString(R.string.main_save_action_on_error), Toast.LENGTH_LONG).show()
    }

    private class SaveThread(val context: Context, val blog: Blog, val article: Article) : Thread() {
        override fun run() {
            Database.getInstance(context).blogDao().run {
                val blogWithArticles: BlogWithArticles? = getBlogWithArticles(blog.blogId)

                blogWithArticles?.run {
                    if (articles.any {
                        it.articleNo == article.articleNo &&
                        it.contentHash == article.contentHash
                    }) {
                        return
                    }
                }

                insertArticleWithBlog(
                    blog,
                    article,
                )
            }
        }
    }

    private class JavaScriptInterface(
        val onSuccess: (Blog, Article) -> Unit,
        val onError: () -> Unit
    ) {
        companion object {
            private const val ogUrlSelector = "meta[property=og:url]"
            private const val titleSelector = "meta[property=og:title]"
            private const val pagePropertySelector = "#_post_property"
            private const val contentSelector = ".se-main-container"
            private val ogUrlRegex = ".+\\.naver\\.com/[a-z0-9_\\-]+/[0-9]+".toRegex()
        }

        @JavascriptInterface
        fun get(html: String) {
            val doc = Jsoup.parse(html)

            val ogUrl = doc.select(ogUrlSelector).attr("content")
            if (!ogUrlRegex.matches(ogUrl)) {
                return
            }

            val (blogId, articleId) = ogUrl.split('/').takeLast(2)
            val title = doc.select(titleSelector).attr("content")
            val blogName = doc.select(pagePropertySelector).attr("blogName")
            val createdAtMillis = doc.select(pagePropertySelector).attr("addDate")
            val content = doc.select(contentSelector).apply { select("br, p").append("\\n") }.text().trim()

            if (arrayOf(blogId, articleId, title, blogName, createdAtMillis, content).any { it.isEmpty() }) {
                return onError()
            }

            val articleNo = articleId.toLong()
            val createdAt = Date(createdAtMillis.toLong())

            onSuccess(
                Blog(
                    blogId = blogId,
                    title = blogName,
                ),
                ArticleBuilder(
                    blogId = blogId,
                    articleNo = articleNo,
                    title = title,
                    content = content,
                    createdAt = createdAt,
                ).build(),
            )
        }
    }
}
