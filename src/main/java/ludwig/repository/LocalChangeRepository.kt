package ludwig.repository

import com.fasterxml.jackson.core.JsonEncoding
import ludwig.changes.Change
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

class LocalChangeRepository(private val file: File) : ChangeRepository {

    @Throws(IOException::class)
    override fun push(changes: List<Change<*>>) {
        FileOutputStream(file, true).use { fos ->
            YamlConfiguration.YAML_FACTORY.createGenerator(fos, JsonEncoding.UTF8).use { generator ->
                for (change in changes) {
                    YamlConfiguration.OBJECT_MAPPER.writeValue(generator, change)
                }
            }
        }
    }

    @Throws(IOException::class)
    override fun pull(sinceChangeId: String?): List<Change<*>> {
        YamlConfiguration.YAML_FACTORY.createParser(file).use { parser ->
            YamlConfiguration.OBJECT_MAPPER.readValues(parser, Change::class.java).use { it ->
                val changes = ArrayList<Change<*>>()
                var accept = sinceChangeId == null
                while (it.hasNext()) {
                    val change = it.nextValue()
                    if (accept) {
                        changes.add(change)
                    }
                    accept = accept || change.changeId == sinceChangeId
                }
                return changes
            }
        }
    }
}
