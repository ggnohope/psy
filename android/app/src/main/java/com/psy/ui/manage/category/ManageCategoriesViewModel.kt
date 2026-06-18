package com.psy.ui.manage.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.psy.domain.model.Category
import com.psy.domain.model.CategoryType
import com.psy.domain.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ManageCategoriesUiState(
    val type: CategoryType = CategoryType.EXPENSE,
    val categories: List<Category> = emptyList(),
    // editor
    val editorOpen: Boolean = false,
    val editingId: Long? = null,
    val draftName: String = "",
    val draftIcon: String = "📦",
    val draftColor: Long = 0xFFA18CFF,
    // delete
    val pendingDelete: Category? = null,
)

@HiltViewModel
class ManageCategoriesViewModel @Inject constructor(
    private val repo: CategoryRepository,
) : ViewModel() {

    private val _type = MutableStateFlow(CategoryType.EXPENSE)
    private val _editorOpen = MutableStateFlow(false)
    private val _editingId = MutableStateFlow<Long?>(null)
    private val _draftName = MutableStateFlow("")
    private val _draftIcon = MutableStateFlow("📦")
    private val _draftColor = MutableStateFlow<Long>(0xFFA18CFFL)
    private val _pendingDelete = MutableStateFlow<Category?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _categories = _type.flatMapLatest { type ->
        repo.observeByType(type)
    }

    val uiState: StateFlow<ManageCategoriesUiState> = combine(
        _type,
        _categories,
        _editorOpen,
        _editingId,
        _draftName,
        _draftIcon,
        _draftColor,
        _pendingDelete,
    ) { values ->
        // combine with 8 flows: values is Array<Any?>
        val type = values[0] as CategoryType
        @Suppress("UNCHECKED_CAST")
        val categories = values[1] as List<Category>
        val editorOpen = values[2] as Boolean
        val editingId = values[3] as Long?
        val draftName = values[4] as String
        val draftIcon = values[5] as String
        val draftColor = values[6] as Long
        val pendingDelete = values[7] as Category?
        ManageCategoriesUiState(
            type = type,
            categories = categories,
            editorOpen = editorOpen,
            editingId = editingId,
            draftName = draftName,
            draftIcon = draftIcon,
            draftColor = draftColor,
            pendingDelete = pendingDelete,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ManageCategoriesUiState(),
    )

    // ── Tab ──────────────────────────────────────────────────────────────────

    fun selectTab(type: CategoryType) {
        _type.value = type
    }

    // ── Editor ───────────────────────────────────────────────────────────────

    fun startAdd() {
        _editingId.value = null
        _draftName.value = ""
        _draftIcon.value = "📦"
        _draftColor.value = 0xFFA18CFFL
        _editorOpen.value = true
    }

    fun startEdit(category: Category) {
        _editingId.value = category.id
        _draftName.value = category.name
        _draftIcon.value = category.icon
        _draftColor.value = category.color
        _editorOpen.value = true
    }

    fun onNameChange(name: String) {
        _draftName.value = name
    }

    fun onIconChange(icon: String) {
        _draftIcon.value = icon
    }

    fun onColorChange(color: Long) {
        _draftColor.value = color
    }

    fun closeEditor() {
        _editorOpen.value = false
    }

    fun saveEditor() {
        val currentCategories = uiState.value.categories
        val maxSortOrder = currentCategories.maxOfOrNull { it.sortOrder } ?: -1
        val isNew = _editingId.value == null

        val category = Category(
            id = _editingId.value ?: 0L,
            name = _draftName.value.trim(),
            icon = _draftIcon.value,
            color = _draftColor.value,
            type = _type.value,
            sortOrder = if (isNew) maxSortOrder + 1 else {
                // keep existing sortOrder
                currentCategories.find { it.id == _editingId.value }?.sortOrder ?: (maxSortOrder + 1)
            },
        )
        viewModelScope.launch {
            repo.upsert(category)
            _editorOpen.value = false
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    fun requestDelete(category: Category) {
        _pendingDelete.value = category
    }

    fun cancelDelete() {
        _pendingDelete.value = null
    }

    fun confirmDelete() {
        val category = _pendingDelete.value ?: return
        viewModelScope.launch {
            repo.delete(category)
            _pendingDelete.value = null
        }
    }
}
