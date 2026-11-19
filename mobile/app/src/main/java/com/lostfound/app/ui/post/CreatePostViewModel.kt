package com.lostfound.app.ui.post

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lostfound.app.core.location.LatLng
import com.lostfound.app.core.network.ApiException
import com.lostfound.app.core.util.Formatters
import com.lostfound.app.core.util.userMessage
import com.lostfound.app.data.model.Category
import com.lostfound.app.data.repository.PostDraft
import com.lostfound.app.data.repository.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.ZonedDateTime

const val MAX_IMAGES = 5
const val MAX_TAGS = 10
const val MAX_QUESTIONS = 3

data class CreatePostState(
    val type: String = "FOUND",
    val images: List<Uri> = emptyList(),
    val title: String = "",
    val categories: List<Category> = emptyList(),
    val categoryId: Int? = null,
    val tags: List<String> = emptyList(),
    val tagInput: String = "",
    val locationText: String = "",
    val location: LatLng? = null,
    val locating: Boolean = false,
    val eventTime: ZonedDateTime = ZonedDateTime.now(ZoneId.systemDefault()).withSecond(0).withNano(0),
    val description: String = "",
    val questions: List<String> = listOf(""),
    val errors: Map<String, String> = emptyMap(),
    val error: String? = null,
    val submitting: Boolean = false,
    val progress: String? = null,
    val createdPostId: String? = null
) {
    val isFound: Boolean get() = type == "FOUND"
}

class CreatePostViewModel : ViewModel() {
    private val _state = MutableStateFlow(CreatePostState())
    val state = _state.asStateFlow()

    init {
        loadCategories()
    }

    fun loadCategories() {
        viewModelScope.launch {
            try {
                val list = PostRepository.categories()
                _state.update { it.copy(categories = list) }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Không tải được danh mục: ${e.userMessage()}") }
            }
        }
    }

    private fun edit(field: String? = null, transform: (CreatePostState) -> CreatePostState) {
        _state.update { s ->
            // Xóa lỗi cũ của ô đang sửa trước, để transform có thể đặt lỗi mới
            val base = if (field == null) s else s.copy(errors = s.errors - field)
            transform(base).copy(error = null)
        }
    }

    fun setType(type: String) = edit("questions") {
        // Bài Found bắt buộc có ít nhất 1 câu hỏi: đảm bảo luôn có 1 ô trống để nhập
        it.copy(type = type, questions = if (type == "FOUND" && it.questions.isEmpty()) listOf("") else it.questions)
    }

    fun addImages(uris: List<Uri>) = edit("images") {
        it.copy(images = (it.images + uris).distinct().take(MAX_IMAGES))
    }

    fun removeImage(uri: Uri) = edit("images") { it.copy(images = it.images - uri) }

    fun setTitle(v: String) = edit("title") { it.copy(title = v) }
    fun setCategory(id: Int) = edit("category") { it.copy(categoryId = id) }
    fun setDescription(v: String) = edit("description") { it.copy(description = v) }
    fun setLocationText(v: String) = edit("location") { it.copy(locationText = v) }
    fun clearCoordinates() = edit { it.copy(location = null) }

    fun setEventTime(time: ZonedDateTime) = edit("time") { it.copy(eventTime = time) }

    fun onTagInput(v: String) {
        if (v.endsWith(",")) addTag(v.dropLast(1)) else edit("tags") { it.copy(tagInput = v) }
    }

    fun addTag(raw: String = _state.value.tagInput) {
        val tag = raw.trim().lowercase().replace(Regex("\\s+"), " ")
        edit("tags") { s ->
            when {
                tag.isEmpty() -> s.copy(tagInput = "")
                tag.length > 30 -> s.copy(errors = s.errors + ("tags" to "Mỗi tag tối đa 30 ký tự"))
                s.tags.size >= MAX_TAGS -> s.copy(tagInput = "", errors = s.errors + ("tags" to "Tối đa $MAX_TAGS tag"))
                tag in s.tags -> s.copy(tagInput = "")
                else -> s.copy(tags = s.tags + tag, tagInput = "")
            }
        }
    }

    fun removeTag(tag: String) = edit("tags") { it.copy(tags = it.tags - tag) }

    fun setQuestion(index: Int, v: String) = edit("questions") {
        it.copy(questions = it.questions.mapIndexed { i, q -> if (i == index) v else q })
    }

    fun addQuestion() = edit("questions") {
        if (it.questions.size >= MAX_QUESTIONS) it else it.copy(questions = it.questions + "")
    }

    fun removeQuestion(index: Int) = edit("questions") {
        it.copy(questions = it.questions.filterIndexed { i, _ -> i != index })
    }

    fun setLocating(v: Boolean) = _state.update { it.copy(locating = v) }

    /** Đã lấy được GPS (và gợi ý tên địa điểm nếu ô đang trống). */
    fun setCoordinates(point: LatLng?, suggestedAddress: String?) {
        _state.update { s ->
            if (point == null) {
                s.copy(locating = false, error = "Không lấy được vị trí. Hãy bật định vị và cho phép quyền vị trí.")
            } else {
                s.copy(
                    locating = false,
                    location = point,
                    locationText = if (s.locationText.isBlank() && suggestedAddress != null) suggestedAddress else s.locationText,
                    errors = s.errors - "location"
                )
            }
        }
    }

    /** Kiểm tra giống luật của POST /posts để báo lỗi ngay trên form. */
    private fun validate(s: CreatePostState): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        val title = s.title.trim()
        if (title.length < 3) errors["title"] = "Tiêu đề tối thiểu 3 ký tự"
        if (title.length > 120) errors["title"] = "Tiêu đề tối đa 120 ký tự"
        if (s.categoryId == null) errors["category"] = "Chọn danh mục"
        val loc = s.locationText.trim()
        if (loc.length < 2) errors["location"] = "Nhập địa điểm (tối thiểu 2 ký tự)"
        if (loc.length > 200) errors["location"] = "Địa điểm tối đa 200 ký tự"
        if (s.description.trim().length > 2000) errors["description"] = "Mô tả tối đa 2000 ký tự"
        if (s.eventTime.isAfter(ZonedDateTime.now().plusMinutes(5))) {
            errors["time"] = "Thời gian không được ở tương lai"
        }
        val questions = s.questions.map { it.trim() }.filter { it.isNotEmpty() }
        when {
            s.isFound && questions.isEmpty() ->
                errors["questions"] = "Bài Found cần ít nhất 1 câu hỏi xác minh"
            questions.any { it.length < 3 } -> errors["questions"] = "Mỗi câu hỏi tối thiểu 3 ký tự"
            questions.any { it.length > 300 } -> errors["questions"] = "Mỗi câu hỏi tối đa 300 ký tự"
        }
        return errors
    }

    fun submit(context: Context) {
        val s = _state.value
        if (s.submitting) return
        // Tag đang gõ dở mà chưa bấm Thêm thì vẫn tính
        val pendingTag = s.tagInput.trim().lowercase().replace(Regex("\\s+"), " ")
        val tags = if (pendingTag.isNotEmpty() && pendingTag !in s.tags && s.tags.size < MAX_TAGS && pendingTag.length <= 30) {
            s.tags + pendingTag
        } else s.tags

        val errors = validate(s)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors, error = "Vui lòng kiểm tra lại các mục được đánh dấu") }
            return
        }
        val draft = PostDraft(
            type = s.type,
            title = s.title,
            description = s.description,
            categoryId = s.categoryId!!,
            locationText = s.locationText,
            latitude = s.location?.lat,
            longitude = s.location?.lng,
            eventTimeIso = Formatters.toIso(s.eventTime),
            tags = tags,
            questions = s.questions.map { it.trim() }.filter { it.isNotEmpty() },
            images = s.images
        )
        _state.update { it.copy(submitting = true, error = null, progress = "Đang chuẩn bị…", tags = tags, tagInput = "") }
        val appContext = context.applicationContext
        viewModelScope.launch {
            try {
                val id = PostRepository.createPost(appContext, draft) { step ->
                    _state.update { it.copy(progress = step) }
                }
                _state.update { it.copy(submitting = false, progress = null, createdPostId = id) }
            } catch (e: ApiException) {
                val msg = when (e.status) {
                    429 -> "Bạn đã đăng tối đa 10 bài trong 24 giờ. Vui lòng thử lại sau."
                    else -> e.userMessage()
                }
                _state.update { it.copy(submitting = false, progress = null, error = msg) }
            } catch (e: Exception) {
                _state.update { it.copy(submitting = false, progress = null, error = e.userMessage()) }
            }
        }
    }
}
