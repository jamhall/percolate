package eu.jamiehall.percolate.domain

import java.io.Serializable
import java.util.*

class File(val name: String,
           val description: String?,
           val path: String,
           val size: Long,
           val type: FileType,
           val createdAt: Date,
           val owner: String?,
           val writable: Boolean) : Serializable