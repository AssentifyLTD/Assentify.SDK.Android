data class DocumentTemplatesModel(
    val id: Int,
    val documentId: Int,
    val name: String,
    val dateCreated: String,
    val dateModified: String,
    val templatePath: String,
    val thumbnailFilePath: String,
)