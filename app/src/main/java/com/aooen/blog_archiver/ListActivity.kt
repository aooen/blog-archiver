package com.aooen.blog_archiver

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.aooen.blog_archiver.database.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

private const val EXPORT_FILE_FOLDER = "exports"
private const val EXPORT_FILE_NAME = "blog.db"

class ListActivity : AppCompatActivity() {
    private lateinit var blogDao: BlogDao
    private lateinit var blogList: List<Blog>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        blogDao = Database.getInstance(this).blogDao()
        blogList = blogDao.getBlogs()

        supportActionBar?.run {
            title = getString(R.string.list_title_main)
            setDisplayHomeAsUpEnabled(true)
        }

        val onBlogClicked = OnBlogClicked()
        val blogListLayout = LinearLayout(this).apply {
            addView(ListView(this@ListActivity).apply {
                adapter = ArrayAdapter(this@ListActivity, android.R.layout.simple_list_item_1, blogList.map { it.title })
                onItemClickListener = onBlogClicked
            })
        }

        setContentView(blogListLayout)
    }

    private inner class OnBlogClicked : AdapterView.OnItemClickListener {
        override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            val blog = blogList[position]
            val contentList = blogDao.getBlogWithArticles(blog.blogId)?.articles?.sortedWith(compareBy<Article> { it.articleNo }.thenByDescending { it.archivedAt })

            supportActionBar?.title = blog.title

            if (contentList == null) {
                return
            }

            val onContentClicked = OnContentClicked(contentList)
            val contentListLayout = LinearLayout(this@ListActivity).apply {
                addView(ListView(this@ListActivity).apply {
                    adapter = ArrayAdapter(this@ListActivity, android.R.layout.simple_list_item_1, contentList.map {
                        val date = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(it.createdAt)
                        "$date ${it.title}"
                    })
                    onItemClickListener = onContentClicked
                })
            }

            setContentView(contentListLayout)
        }
    }

    private inner class OnContentClicked(val contentList: List<Article>) : AdapterView.OnItemClickListener {
        override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            val content = contentList[position]

            supportActionBar?.title = content.title

            val contentListLayout = ScrollView(this@ListActivity).apply {
                addView(TextView(this@ListActivity).apply {
                    text = content.content.replace("\\n", "\n")
                })
            }

            setContentView(contentListLayout)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.list_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.exportAction -> {
                val dbFile = getDatabasePath(DB_NAME)
                val exportFile = File(getExternalFilesDir(EXPORT_FILE_FOLDER), EXPORT_FILE_NAME)

                Database.getInstance(this).close()

                if (exportFile.exists()) {
                    Toast.makeText(this, getString(R.string.list_export_error_exist, exportFile.path), Toast.LENGTH_LONG).show()
                    return true
                } else {
                    exportFile.parentFile?.mkdirs()
                }

                FileInputStream(dbFile).use { inputStream ->
                    FileOutputStream(exportFile).use { outputStream ->
                        val buf = ByteArray(1024)
                        while (true) {
                            val len = inputStream.read(buf)
                            if (len <= 0) {
                                break
                            }

                            outputStream.write(buf, 0, len)
                        }
                    }
                }

                Toast.makeText(this, getString(R.string.list_export_success, exportFile.path), Toast.LENGTH_LONG).show()
                return true
            }
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
    }
}