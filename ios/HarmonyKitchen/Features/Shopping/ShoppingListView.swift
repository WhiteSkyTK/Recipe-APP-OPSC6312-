import SwiftUI

struct ShoppingListView: View {
    @Environment(ShoppingStore.self) private var shopping
    @Environment(ToastCenter.self) private var toasts

    @State private var selection: ShoppingSelection = .all
    @State private var editMode: EditMode = .inactive
    @State private var selectedIDs: Set<String> = []
    @State private var showAddSheet = false
    @State private var showDeleteList = false

    private var selectedList: ShoppingList? {
        if case .list(let id) = selection { return shopping.list(id: id) }
        return nil
    }

    private var title: String {
        guard let list = selectedList else { return L("shopping_list_all_items") }
        return list.isMyList ? L("shopping_list_my_list") : "\(list.emoji) \(list.name)"
    }

    /// New items go to the selected list, or to "My List" from "All Items" (as on Android).
    private var targetListID: String { selectedList?.id ?? ShoppingList.myListID }

    var body: some View {
        let items = shopping.items(for: selection)
        List(selection: $selectedIDs) {
            ForEach(items) { item in
                row(for: item)
                    .tag(item.id)
            }
            .onDelete { offsets in
                let removed = offsets.map { items[$0] }
                shopping.delete(removed)
                if removed.count == 1, let item = removed.first {
                    toasts.show(LF("shopping_list_item_deleted_snackbar", item.name), actionTitle: L("undo")) {
                        shopping.restore(item)
                    }
                }
            }
        }
        .environment(\.editMode, $editMode)
        .overlay {
            if items.isEmpty {
                ContentUnavailableView(L("ios_list_empty_title"), systemImage: "cart", description: Text(L("ios_list_empty_message")))
            }
        }
        .navigationTitle(title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbarTitleMenu { listMenu }
        .toolbar { toolbarContent(itemCount: items.count) }
        .sheet(isPresented: $showAddSheet) {
            AddItemsSheet { names in
                let added = await shopping.addItems(names, toList: targetListID)
                toasts.show(LP("shopping_list_toast_items_added", added))
            }
        }
        .alert(L("shopping_list_delete_dialog_title"), isPresented: $showDeleteList) {
            Button(L("delete"), role: .destructive) {
                if let list = selectedList {
                    shopping.deleteList(list)
                    selection = .list(ShoppingList.myListID)
                }
            }
            Button(L("cancel"), role: .cancel) {}
        } message: {
            Text(L("shopping_list_delete_dialog_message"))
        }
        .onChange(of: selection) { _, _ in
            selectedIDs = []
            editMode = .inactive
        }
    }

    @ViewBuilder
    private func row(for item: ShoppingItem) -> some View {
        let label = HStack(spacing: 12) {
            Image(systemName: item.isChecked ? "checkmark.circle.fill" : "circle")
                .font(.title3)
                .foregroundStyle(item.isChecked ? Color.brand : .secondary)
            Text(item.name)
                .strikethrough(item.isChecked)
                .foregroundStyle(item.isChecked ? .secondary : .primary)
            Spacer()
            if case .all = selection, let list = shopping.list(id: item.ownerListId), !list.isMyList {
                Text(list.emoji).font(.caption)
            }
        }
        .contentShape(Rectangle())

        if editMode.isEditing {
            label
        } else {
            Button { shopping.setChecked(item, !item.isChecked) } label: { label }
                .buttonStyle(.plain)
                .sensoryFeedback(.selection, trigger: item.isChecked)
        }
    }

    @ViewBuilder
    private var listMenu: some View {
        Picker(L("shopping_list"), selection: $selection) {
            Text(L("shopping_list_all_items")).tag(ShoppingSelection.all)
            ForEach(shopping.lists) { list in
                Text(list.isMyList ? L("shopping_list_my_list") : "\(list.emoji) \(list.name)")
                    .tag(ShoppingSelection.list(list.id))
            }
        }
        if let list = selectedList, !list.isMyList {
            Button(L("delete_list"), role: .destructive) { showDeleteList = true }
        }
    }

    @ToolbarContentBuilder
    private func toolbarContent(itemCount: Int) -> some ToolbarContent {
        ToolbarItem(placement: .topBarLeading) {
            if itemCount > 0 {
                Button(editMode.isEditing ? L("ios_done") : L("ios_select")) {
                    withAnimation {
                        editMode = editMode.isEditing ? .inactive : .active
                        selectedIDs = []
                    }
                }
            }
        }
        ToolbarItem(placement: .topBarTrailing) {
            if editMode.isEditing {
                Button(role: .destructive) {
                    let items = shopping.items(for: selection).filter { selectedIDs.contains($0.id) }
                    shopping.delete(items)
                    selectedIDs = []
                    editMode = .inactive
                } label: {
                    Image(systemName: "trash")
                }
                .disabled(selectedIDs.isEmpty)
                .accessibilityLabel(LP("shopping_list_selection_count", selectedIDs.count))
            } else {
                Button { showAddSheet = true } label: {
                    Image(systemName: "plus")
                }
                .accessibilityLabel(L("shopping_list_add_items_title"))
            }
        }
    }
}

/// Adds several items at once, one per line.
struct AddItemsSheet: View {
    var onAdd: ([String]) async -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var text = ""
    @FocusState private var focused: Bool

    var body: some View {
        NavigationStack {
            TextEditor(text: $text)
                .focused($focused)
                .padding()
                .overlay(alignment: .topLeading) {
                    if text.isEmpty {
                        Text(L("enter_items_one_per_line"))
                            .foregroundStyle(.tertiary)
                            .padding(.horizontal, 21)
                            .padding(.vertical, 24)
                            .allowsHitTesting(false)
                    }
                }
                .navigationTitle(L("shopping_list_add_items_title"))
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button(L("cancel")) { dismiss() }
                    }
                    ToolbarItem(placement: .confirmationAction) {
                        Button(L("add")) {
                            let names = text.split(whereSeparator: \.isNewline)
                                .map { $0.trimmingCharacters(in: .whitespaces) }
                                .filter { !$0.isEmpty }
                            Task {
                                await onAdd(names)
                                dismiss()
                            }
                        }
                        .disabled(text.trimmed.isEmpty)
                    }
                }
                .onAppear { focused = true }
        }
        .presentationDetents([.medium, .large])
    }
}
