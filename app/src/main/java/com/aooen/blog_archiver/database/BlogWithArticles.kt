package com.aooen.blog_archiver.database

import androidx.room.Embedded
import androidx.room.Relation

data class BlogWithArticles(
    @Embedded val blog: Blog,
    @Relation(parentColumn = "blogId", entityColumn = "blogId") val articles: List<Article>,
)