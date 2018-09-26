package eu.jamiehall.percolate

import com.googlecode.cqengine.ConcurrentIndexedCollection
import com.googlecode.cqengine.IndexedCollection
import com.googlecode.cqengine.attribute.support.SimpleFunction
import com.googlecode.cqengine.query.QueryFactory.attribute
import eu.jamiehall.percolate.domain.File
import eu.jamiehall.percolate.domain.FileType
import eu.jamiehall.percolate.domain.FileType.FILE
import eu.jamiehall.percolate.exception.InvalidQueryException
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec
import org.apache.commons.lang3.time.DateUtils.addMinutes
import java.util.*

class FilterParserTest : StringSpec() {
    init {
        val query = FilterQuery(File::class.java)
        with(query) {
            registerAttribute(attribute<File, String>(File::class.java, String::class.java, "name", File::name))
            registerAttribute(attribute<File, String>(File::class.java, String::class.java, "description", File::description))
            registerAttribute(attribute<File, String>(File::class.java, String::class.java, "path", File::path))
            registerAttribute(attribute<File, Long>("size", SimpleFunction<File, Long> { it.size }))
            registerAttribute(attribute<File, FileType>(File::class.java, FileType::class.java, "type", File::type))
            registerAttribute(attribute<File, Date>(File::class.java, Date::class.java, "createdAt", File::createdAt))
            registerAttribute(attribute<File, String>(File::class.java, String::class.java, "owner", File::owner))
            registerAttribute(attribute<File, Boolean>("writable", SimpleFunction<File, Boolean> { it.writable }))
        }

        fun createFile(name: String,
                       description: String?,
                       path: String,
                       size: Long,
                       type: FileType,
                       createdAt: Date,
                       owner: String?,
                       writable: Boolean): File {
            return File(name, description, path, size, type, createdAt, owner, writable)
        }

        fun createFixtures(): IndexedCollection<File> {
            val incidents = ConcurrentIndexedCollection<File>()
            with(incidents) {
                add(createFile("file1.txt", null, "/tmp", 1000000, FILE, Date(), null, true))
                add(createFile("file2.txt", null, "/tmp", 1500000, FILE, Date(), "hall", true))
                add(createFile("file3.txt", null, "/tmp", 2000000, FILE, addMinutes(Date(), -11), "hall", true))
                add(createFile("picture1.jpg", null, "/tmp/pictures", 2500000, FILE, addMinutes(Date(), -10), "hall", true))
                add(createFile("picture2.jpg", null, "/tmp/pictures", 3000000, FILE, addMinutes(Date(), -10), "hall", true))
                add(createFile("picture3.jpg", "Jamie's car", "/tmp/pictures", 3500000, FILE, addMinutes(Date(), -10), "hall", false))
            }
            return incidents
        }

        /**
         * Tests
         */
        "should return a file by its name" {
            query.retrieve(createFixtures(), "name = 'file1.txt'").size().shouldBe(1)
        }

        "should return all files in the /tmp directory" {
            val results = query.retrieve(createFixtures(), "path = '/tmp'")
            results.size().shouldBe(3)
        }

        "should return all files that are not in the /tmp/pictures directory" {
            query.retrieve(createFixtures(), "path NOT IN ('/tmp/pictures')").size().shouldBe(3)
        }

        "should return all files that match a description" {
            query.retrieve(createFixtures(), "description = 'Jamie\\'s car'").size().shouldBe(1)
        }

        "should return all files that are not in the /tmp directory" {
            query.retrieve(createFixtures(), "path <> '/tmp'").size().shouldBe(3)
            query.retrieve(createFixtures(), "path != '/tmp'").size().shouldBe(3)
        }

        "should return all files in the /tmp/pictures directory" {
            query.retrieve(createFixtures(), "path = '/tmp/pictures'").size().shouldBe(3)
        }

        "should return all files greater than 2MB" {
            query.retrieve(createFixtures(), "size > '2MB'").size().shouldBe(3)
            query.retrieve(createFixtures(), "size > 2000000").size().shouldBe(3)
            query.retrieve(createFixtures(), "size > '2000000'").size().shouldBe(3)
        }

        "should return all files between 1MB and 2MB" {
            query.retrieve(createFixtures(), "size BETWEEN '1MB' AND '2MB'").size().shouldBe(3)
        }

        "should not return any files greater than 10M" {
            query.retrieve(createFixtures(), "size > '10MB'").size().shouldBe(0)
        }

        "should return all files of type FILE" {
            query.retrieve(createFixtures(), "type = 'FILE'").size().shouldBe(6)
            query.retrieve(createFixtures(), "type IN ('FILE')").size().shouldBe(6)
        }

        "should return all files where path is equal to /tmp and file size greater than 1MB" {
            query.retrieve(createFixtures(), "path = '/tmp' AND size > '1MB'").size().shouldBe(2)
            query.retrieve(createFixtures(), "path = '/tmp' AND size > 1000000").size().shouldBe(2)
        }

        "should return all files in the past 5 to 10 minutes" {
            query.retrieve(createFixtures(), "createdAt <= '-5MINUTES' AND createdAt >= '-10MINUTES'").size().shouldBe(3)
            query.retrieve(createFixtures(), "createdAt BETWEEN '-10MINUTES' AND '-5MINUTES'").size().shouldBe(3)
        }

        "should return all files that have no owner" {
            query.retrieve(createFixtures(), "owner IS NULL").size().shouldBe(1)
        }

        "should return all files that have an owner" {
            query.retrieve(createFixtures(), "owner IS NOT NULL").size().shouldBe(5)
        }

        "should return all files for a given owner" {
            query.retrieve(createFixtures(), "owner = 'hall'").size().shouldBe(5)
        }

        "should return all files that are writable" {
            query.retrieve(createFixtures(), "writable = true").size().shouldBe(5)
        }

        "should return all file names that start with picture" {
            query.retrieve(createFixtures(), "name LIKE 'picture%'").size().shouldBe(3)
        }

        "should return all file names that end with jpg" {
            query.retrieve(createFixtures(), "name LIKE '%.jpg'").size().shouldBe(3)
        }

        "should fail because of duplicate query" {
            shouldThrow<InvalidQueryException> {
                query.retrieve(createFixtures(), "name LIKE 'picture%' name LIKE 'picture%")
            }
        }

        "should fail because of leading gibberish" {
            shouldThrow<InvalidQueryException> {
                query.retrieve(createFixtures(), "abc name LIKE 'picture%'")
            }
        }

        "should fail because of unclosed query" {
            shouldThrow<InvalidQueryException> {
                query.retrieve(createFixtures(), "name LIKE 'picture%' AND")
            }
        }

        "should fail because of unknown attribute" {
            shouldThrow<InvalidQueryException> {
                query.retrieve(createFixtures(), "apple ='hello'")
            }
        }

        "should fail because of null query" {
            shouldThrow<InvalidQueryException> {
                query.retrieve(createFixtures(), null)
            }
        }

        "should accept comments in query" {
            query.retrieve(createFixtures(), """
                -- Get all files that do not have an owner
                owner IS NULL
                """)
            query.retrieve(createFixtures(), """
               /**
                * Get all files that do not have an owner
                */
                owner IS NULL
                """)
        }
    }

}