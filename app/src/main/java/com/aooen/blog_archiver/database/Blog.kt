package com.aooen.blog_archiver.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Blog(
    @PrimaryKey val blogId: String,
    val title: String,
)