package com.xiaocydx.inputview.sample.transform.figure

data class Figure(
    val id: String,
    val coverUrl: String,
    val coverRatio: Float,
    val dubbing: Dubbing = Dubbing()
)

data class Dubbing(val id: String = "", val name: String = "配音")

object FigureSource {
    private val coverUrls = listOf(
        Url("https://cdn.pixabay.com/photo/2024/02/15/15/02/reading-8575569_1280.jpg", 1280, 1280),
        Url("https://cdn.pixabay.com/photo/2024/04/08/22/01/ai-generated-8684629_1280.jpg", 853, 1280),
        Url("https://cdn.pixabay.com/photo/2024/04/11/07/17/ai-generated-8689332_1280.png", 960, 1280),
        Url("https://cdn.pixabay.com/photo/2023/12/27/07/50/ai-generated-8471595_960_720.jpg", 960, 720),
        Url("https://cdn.pixabay.com/photo/2024/04/11/18/47/ai-generated-8690368_1280.jpg", 1024, 1280),
        Url("https://cdn.pixabay.com/photo/2024/04/06/02/44/ai-generated-8678498_1280.png", 1280, 1280),
        Url("https://cdn.pixabay.com/photo/2023/03/29/01/56/ai-generated-7884416_960_720.jpg", 960, 720),
        Url("https://cdn.pixabay.com/photo/2024/02/17/16/08/ai-generated-8579697_1280.jpg", 1280, 1280),
        Url("https://cdn.pixabay.com/photo/2024/04/04/21/13/ai-generated-8675958_960_720.jpg", 520, 720),
    )

    fun generateList(size: Int) = (1..size).map {
        Figure(
            id = it.toString(),
            coverUrl = coverUrls[it % coverUrls.size].value,
            coverRatio = coverUrls[it % coverUrls.size].ratio
        )
    }

    private data class Url(val value: String, val width: Int, val height: Int) {
        val ratio = width.toFloat() / height
    }
}