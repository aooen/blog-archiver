package com.aooen.blog_archiver.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.security.MessageDigest
import java.util.*

@Entity(foreignKeys = [ForeignKey(
    entity = Blog::class,
    parentColumns = ["blogId"],
    childColumns = ["blogId"],
    onUpdate = ForeignKey.NO_ACTION,
    onDelete = ForeignKey.RESTRICT,
)])
data class Article(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(index = true) val blogId: String,
    @ColumnInfo(index = true) val articleNo: Long,
    val title: String,
    val content: String,
    val contentHash: String,
    val createdAt: Date,
    val archivedAt: Date,
)

data class ArticleBuilder(
    val blogId: String,
    val articleNo: Long,
    val title: String,
    val content: String,
    val createdAt: Date,
) {
    fun build(): Article {
        val contentHash = (
                MessageDigest
                    .getInstance("MD5")
                    .digest(content.toByteArray())
                    .joinToString(separator = "") { byte -> "%02x".format(byte) }
                )
        val archivedAt = Date()
        return Article(
            blogId = blogId,
            articleNo = articleNo,
            title = title,
            content = content,
            contentHash = contentHash,
            createdAt = createdAt,
            archivedAt = archivedAt,
        )
    }
}