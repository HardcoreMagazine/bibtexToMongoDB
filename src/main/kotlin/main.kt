import com.mongodb.client.MongoCollection
import org.jbibtex.BibTeXDatabase
import org.jbibtex.CharacterFilterReader
import org.jbibtex.BibTeXParser
import org.jbibtex.BibTeXEntry
import java.io.File
import java.io.FileReader
import java.io.Reader
import com.mongodb.client.MongoDatabase
import org.json.JSONObject
import org.litote.kmongo.KMongo
import org.litote.kmongo.json
import org.litote.kmongo.getCollection

/** Create mongodb client and return collection from it  **/
private fun manageMongo(): MongoCollection<BibtexFormat> {
    val mdbClient = KMongo.createClient("mongodb://root:password@IP:27017")
    //inside Mongo VM: sudo ufw allow 27017
    val mongoDatabase: MongoDatabase = mdbClient.getDatabase("bibtex")
    return mongoDatabase.getCollection<BibtexFormat>().apply { drop() }
}

/** Get user input (path to file) **/
private fun getUserInfo(): String? {
    println("Enter full file name (ex.: src/main/resources/fft.bib):")
    return readLine()
}

/** Read file if fileNamePath exists **/
fun readFromFile(fileNamePath: String): Reader? {
    var result:Reader? = null
    try {
        result = FileReader(File(fileNamePath))
    } catch (anyException: java.lang.Exception) {
        println("[ERROR] Unable to find/read '$fileNamePath'")
    }
    return result
}

/** Converts Reader to BibTeXDatabase using jbibtex tools**/
fun toBibtex(reader: Reader): BibTeXDatabase? {
    var database: BibTeXDatabase? = null
    val filterReader = CharacterFilterReader(reader)
    //filters all unknown/unreadable symbols - ex.: "®"
    try {
        database = BibTeXParser().parse(filterReader)
        //Throws an exception if *reader* have entries with special keywords, such as "for"
    } catch (e: java.lang.Exception) {
        println("[ERROR] Cannot parse document: publication ID contains values/strings that marked as key words in Kotlin/Java;\n" +
                "Try: adding '_' or '' over 'for', 'is', 'in', 'as' in publication ID inside selected file")
    }
    return database
}

/** exports BibTeXDatabase values to mutable list of BibtexFormat **/
private fun databaseToBibtexFormat(database: BibTeXDatabase) : MutableList<BibtexFormat> {
    val result = mutableListOf<BibtexFormat>()
    database.entries.forEach { (key, entry) ->
        result.add(
                BibtexFormat(
                    type = entry.type.value,
                    ID = key.value,
                    journal = entry.getField(BibTeXEntry.KEY_JOURNAL)?.toUserString(),
                    title = entry.getField(BibTeXEntry.KEY_TITLE).toUserString(),
                    DOI = entry.getField(BibTeXEntry.KEY_DOI)?.toUserString(),
                    author = entry.getField(BibTeXEntry.KEY_AUTHOR)?.toUserString(),
                    year = entry.getField(BibTeXEntry.KEY_YEAR)?.toUserString(),
                    publisher = entry.getField(BibTeXEntry.KEY_PUBLISHER)?.toUserString()
                ) //fully modifiable list
        )
    }
    return result
}

/** prints mdbCollection in readable format **/
fun prettyPrintJson(json: String) = println(JSONObject(json).toString(4))

/** converts mdbCollection to readable format **/
fun prettyPrintCursor(cursor: Iterable<*>) =
    prettyPrintJson("{ result: ${cursor.json} }")


fun main(){
    val reader = getUserInfo()?.let { readFromFile(it) }
    val database = reader?.let { toBibtex(it) }
    reader?.close()
    val converted = database?.let { databaseToBibtexFormat(it) }
    val mdbCollection = manageMongo()
    if (converted != null) {
        mdbCollection.insertMany(converted)
        println("Print export results (y/n)?")
        when (readLine()) {
            "y" -> {
                println("Print all exported entries (y/n)?")
                when (readLine()) {
                    "y" -> {
                        println("\n------read------")
                        prettyPrintCursor(mdbCollection.find())
                        //full output
                    }
                    else -> {
                        println("\n------read------")
                        prettyPrintCursor(mdbCollection.find().limit(3))
                        //limited output (first 3 documents)
                    }
                }
                print("----------------")
            }
        }
    }
}