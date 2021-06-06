package com.aooen.blog_archiver.database

import androidx.room.*

@Dao
interface BlogDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertArticleWithBlog(blog: Blog, article: Article)

    @Transaction
    @Query("SELECT * FROM Blog WHERE blogId == :blogId")
    fun getBlogWithArticles(blogId: String): BlogWithArticles?

    @Query("SELECT * FROM Blog")
    fun getBlogs(): List<Blog>
}