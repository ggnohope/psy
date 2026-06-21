package com.psy.ui.manage.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.psy.domain.model.Category
import com.psy.domain.model.CategoryGroup
import com.psy.domain.model.CategoryType
import com.psy.domain.repository.BudgetRepository
import com.psy.domain.repository.CategoryGroupRepository
import com.psy.domain.repository.CategoryRepository
import com.psy.domain.repository.LedgerRepository
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

/** A parent group together with its leaf categories (already sorted by sortOrder). */
data class GroupWithLeaves(
    val group: CategoryGroup,
    val leaves: List<Category>,
)

data class ManageCategoriesUiState(
    val type: CategoryType = CategoryType.EXPENSE,
    val groups: List<GroupWithLeaves> = emptyList(),
    // group editor
    val groupEditorOpen: Boolean = false,
    val editingGroupId: Long? = null,
    val groupDraftName: String = "",
    val groupDraftIcon: String = "📦",
    val groupDraftColor: Long = 0xFFA18CFF,
    // leaf editor
    val leafEditorOpen: Boolean = false,
    val editingLeafId: Long? = null,
    val leafParentGroupId: Long? = null,
    val leafDraftName: String = "",
    val leafDraftIcon: String = "📦",
    // delete
    val pendingDeleteGroup: CategoryGroup? = null,
    val pendingDeleteLeaf: Category? = null,
    val message: String? = null,
)

@HiltViewModel
class ManageCategoriesViewModel @Inject constructor(
    private val groupRepo: CategoryGroupRepository,
    private val categoryRepo: CategoryRepository,
    private val budgetRepo: BudgetRepository,
    private val ledgerRepo: LedgerRepository,
) : ViewModel() {

    private val _type = MutableStateFlow(CategoryType.EXPENSE)

    // group editor
    private val _groupEditorOpen = MutableStateFlow(false)
    private val _editingGroupId = MutableStateFlow<Long?>(null)
    private val _groupDraftName = MutableStateFlow("")
    private val _groupDraftIcon = MutableStateFlow("📦")
    private val _groupDraftColor = MutableStateFlow(0xFFA18CFFL)

    // leaf editor
    private val _leafEditorOpen = MutableStateFlow(false)
    private val _editingLeafId = MutableStateFlow<Long?>(null)
    private val _leafParentGroupId = MutableStateFlow<Long?>(null)
    private val _leafDraftName = MutableStateFlow("")
    private val _leafDraftIcon = MutableStateFlow("📦")

    // delete
    private val _pendingDeleteGroup = MutableStateFlow<CategoryGroup?>(null)
    private val _pendingDeleteLeaf = MutableStateFlow<Category?>(null)
    private val _message = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _groups: kotlinx.coroutines.flow.Flow<List<GroupWithLeaves>> =
        _type.flatMapLatest { type -> groupRepo.observeByType(type) }
            .let { groupsFlow ->
                combine(groupsFlow, categoryRepo.observeAll()) { groups, allLeaves ->
                    groups
                        .sortedBy { it.sortOrder }
                        .map { group ->
                            GroupWithLeaves(
                                group = group,
                                leaves = allLeaves
                                    .filter { it.groupId == group.id }
                                    .sortedBy { it.sortOrder },
                            )
                        }
                }
            }

    // Group editor flows bundled to stay under combine's arity limits.
    private data class GroupEditorState(
        val open: Boolean,
        val editingId: Long?,
        val name: String,
        val icon: String,
        val color: Long,
    )

    private val _groupEditor = combine(
        _groupEditorOpen,
        _editingGroupId,
        _groupDraftName,
        _groupDraftIcon,
        _groupDraftColor,
    ) { open, id, name, icon, color ->
        GroupEditorState(open, id, name, icon, color)
    }

    private data class LeafEditorState(
        val open: Boolean,
        val editingId: Long?,
        val parentGroupId: Long?,
        val name: String,
        val icon: String,
    )

    private val _leafEditor = combine(
        _leafEditorOpen,
        _editingLeafId,
        _leafParentGroupId,
        _leafDraftName,
        _leafDraftIcon,
    ) { open, id, parent, name, icon ->
        LeafEditorState(open, id, parent, name, icon)
    }

    private data class DeleteState(
        val group: CategoryGroup?,
        val leaf: Category?,
        val message: String?,
    )

    private val _delete = combine(
        _pendingDeleteGroup,
        _pendingDeleteLeaf,
        _message,
    ) { group, leaf, message ->
        DeleteState(group, leaf, message)
    }

    val uiState: StateFlow<ManageCategoriesUiState> = combine(
        _type,
        _groups,
        _groupEditor,
        _leafEditor,
        _delete,
    ) { type, groups, groupEditor, leafEditor, delete ->
        ManageCategoriesUiState(
            type = type,
            groups = groups,
            groupEditorOpen = groupEditor.open,
            editingGroupId = groupEditor.editingId,
            groupDraftName = groupEditor.name,
            groupDraftIcon = groupEditor.icon,
            groupDraftColor = groupEditor.color,
            leafEditorOpen = leafEditor.open,
            editingLeafId = leafEditor.editingId,
            leafParentGroupId = leafEditor.parentGroupId,
            leafDraftName = leafEditor.name,
            leafDraftIcon = leafEditor.icon,
            pendingDeleteGroup = delete.group,
            pendingDeleteLeaf = delete.leaf,
            message = delete.message,
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

    // ── Group editor ──────────────────────────────────────────────────────────

    fun startAddGroup() {
        _editingGroupId.value = null
        _groupDraftName.value = ""
        _groupDraftIcon.value = "📦"
        _groupDraftColor.value = 0xFFA18CFFL
        _groupEditorOpen.value = true
    }

    fun startEditGroup(group: CategoryGroup) {
        _editingGroupId.value = group.id
        _groupDraftName.value = group.name
        _groupDraftIcon.value = group.icon
        _groupDraftColor.value = group.color
        _groupEditorOpen.value = true
    }

    fun onGroupNameChange(name: String) {
        _groupDraftName.value = name
    }

    fun onGroupIconChange(icon: String) {
        _groupDraftIcon.value = icon
    }

    fun onGroupColorChange(color: Long) {
        _groupDraftColor.value = color
    }

    fun closeGroupEditor() {
        _groupEditorOpen.value = false
    }

    fun saveGroup() {
        val currentGroups = uiState.value.groups.map { it.group }
        val maxSortOrder = currentGroups.maxOfOrNull { it.sortOrder } ?: -1
        val isNew = _editingGroupId.value == null

        val group = CategoryGroup(
            id = _editingGroupId.value ?: 0L,
            name = _groupDraftName.value.trim(),
            icon = _groupDraftIcon.value,
            color = _groupDraftColor.value,
            type = _type.value,
            sortOrder = if (isNew) {
                maxSortOrder + 1
            } else {
                currentGroups.find { it.id == _editingGroupId.value }?.sortOrder ?: (maxSortOrder + 1)
            },
        )
        viewModelScope.launch {
            groupRepo.upsert(group)
            _groupEditorOpen.value = false
        }
    }

    // ── Leaf editor ─────────────────────────────────────────────────────────

    fun startAddLeaf(groupId: Long) {
        _editingLeafId.value = null
        _leafParentGroupId.value = groupId
        _leafDraftName.value = ""
        _leafDraftIcon.value = "📦"
        _leafEditorOpen.value = true
    }

    fun startEditLeaf(leaf: Category) {
        _editingLeafId.value = leaf.id
        _leafParentGroupId.value = leaf.groupId
        _leafDraftName.value = leaf.name
        _leafDraftIcon.value = leaf.icon
        _leafEditorOpen.value = true
    }

    fun onLeafNameChange(name: String) {
        _leafDraftName.value = name
    }

    fun onLeafIconChange(icon: String) {
        _leafDraftIcon.value = icon
    }

    fun closeLeafEditor() {
        _leafEditorOpen.value = false
    }

    fun saveLeaf() {
        val parentGroupId = _leafParentGroupId.value ?: return
        val siblings = uiState.value.groups
            .firstOrNull { it.group.id == parentGroupId }
            ?.leaves
            ?: emptyList()
        val maxSortOrder = siblings.maxOfOrNull { it.sortOrder } ?: -1
        val isNew = _editingLeafId.value == null

        val leaf = Category(
            id = _editingLeafId.value ?: 0L,
            groupId = parentGroupId,
            name = _leafDraftName.value.trim(),
            icon = _leafDraftIcon.value,
            sortOrder = if (isNew) {
                maxSortOrder + 1
            } else {
                siblings.find { it.id == _editingLeafId.value }?.sortOrder ?: (maxSortOrder + 1)
            },
        )
        viewModelScope.launch {
            categoryRepo.upsert(leaf)
            _leafEditorOpen.value = false
        }
    }

    // ── Delete leaf ───────────────────────────────────────────────────────────

    fun requestDeleteLeaf(leaf: Category) {
        _pendingDeleteLeaf.value = leaf
    }

    fun cancelDeleteLeaf() {
        _pendingDeleteLeaf.value = null
    }

    fun confirmDeleteLeaf() {
        val leaf = _pendingDeleteLeaf.value ?: return
        viewModelScope.launch {
            val countInGroup = categoryRepo.countByGroup(leaf.groupId)
            if (countInGroup <= 1) {
                _message.value = "Mỗi nhóm phải còn ít nhất 1 mục"
            } else {
                categoryRepo.delete(leaf)
            }
            _pendingDeleteLeaf.value = null
        }
    }

    // ── Delete group (cascade) ─────────────────────────────────────────────────

    fun requestDeleteGroup(group: CategoryGroup) {
        _pendingDeleteGroup.value = group
    }

    fun cancelDeleteGroup() {
        _pendingDeleteGroup.value = null
    }

    fun confirmDeleteGroup() {
        val group = _pendingDeleteGroup.value ?: return
        val leaves = uiState.value.groups
            .firstOrNull { it.group.id == group.id }
            ?.leaves
            ?: emptyList()
        viewModelScope.launch {
            // Cascade: delete all leaves first, then the group itself.
            leaves.forEach { categoryRepo.delete(it) }
            groupRepo.delete(group)
            // Also drop any budget referencing this group so the Budget screen
            // doesn't show a phantom item (no FK enforcement on Budget.groupId).
            ledgerRepo.firstOrNull()?.let { ledger ->
                budgetRepo.findByGroup(ledger.id, group.id)?.let { budgetRepo.removeBudget(it) }
            }
            _pendingDeleteGroup.value = null
        }
    }

    // ── Message ─────────────────────────────────────────────────────────────

    fun clearMessage() {
        _message.value = null
    }
}
