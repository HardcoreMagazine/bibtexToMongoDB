import kotlinx.serialization.Serializable

/**
 * Describes basic bibTeX data, such as "type", "ID", "journal", "title", "DOI"
 * "author(-s)", "year of release", "publisher".
 **/

@Serializable data class BibtexFormat(
    val type:String, //@*type*{ ...  ----book, manual, article etc
    val ID:String, // @*type*{ **bookID**, ...
    val journal:String? = null,
    val title:String, //title = { ... }
    val DOI:String? = null, //DOI = { ... } / DOI == digital object identifier
    val author:String? = null, //author = { ... } ----may contain multiple authors && keys ""and"", "",""
    val year:String? = null, // year = { ... }
    val publisher:String? = null // publisher = { ... }
)