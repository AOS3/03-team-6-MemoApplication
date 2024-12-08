package com.lion.a08_memoapplication.fragment

import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.lion.a08_memoapplication.MainActivity
import com.lion.a08_memoapplication.R
import com.lion.a08_memoapplication.databinding.DialogMemoPasswordBinding
import com.lion.a08_memoapplication.databinding.FragmentSearchMemoBinding
import com.lion.a08_memoapplication.databinding.RowMemoBinding
import com.lion.a08_memoapplication.model.MemoModel
import com.lion.a08_memoapplication.repository.MemoRepository
import com.lion.a08_memoapplication.util.FragmentName
import com.lion.a08_memoapplication.util.MemoListName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class SearchMemoFragment : Fragment() {
    lateinit var fragmentSearchMemoBinding: FragmentSearchMemoBinding
    lateinit var mainActivity: MainActivity
    var searchMemoList = mutableListOf<MemoModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        fragmentSearchMemoBinding = FragmentSearchMemoBinding.inflate(layoutInflater)
        mainActivity = activity as MainActivity
        settingToolbar()
        settingRecyclerViewSearch()
        searchMemoData()
        return fragmentSearchMemoBinding.root
    }

    override fun onResume() {
        super.onResume()
        val searchKeyword = fragmentSearchMemoBinding.textFieldSearch.editText?.text.toString()
        CoroutineScope(Dispatchers.Main).launch {
            val work1 = async(Dispatchers.IO) {
                MemoRepository.searchMemoDataByTitle(mainActivity, searchKeyword)
            }
            searchMemoList = if (searchKeyword.isEmpty()) {
                emptyList<MemoModel>().toMutableList()
            } else {
                work1.await()
            }
            fragmentSearchMemoBinding.recyclerSearch.adapter?.notifyDataSetChanged()
        }
    }

    fun settingToolbar() {
        fragmentSearchMemoBinding.materialToolbarSearch.apply {
            title = "메모 검색"
            setNavigationIcon(R.drawable.arrow_back_24px)
            setNavigationOnClickListener {
                mainActivity.removeFragment(FragmentName.SEARCH_MEMO_FRAGMENT)
            }
        }
    }

    fun searchMemoData() {
        fragmentSearchMemoBinding.apply {
            // 검색창에 포커스를 준다.
            mainActivity.showSoftInput(textFieldSearch.editText!!)

            // 키보드의 엔터를 누르면 동작하는 리스너
            textFieldSearch.editText?.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                    val searchMemoTitle = textFieldSearch.editText?.text.toString()
                    CoroutineScope(Dispatchers.Main).launch {
                        val work1 = async(Dispatchers.IO) {
                            MemoRepository.searchMemoDataByTitle(mainActivity, searchMemoTitle)
                        }
                        searchMemoList = if (searchMemoTitle.isEmpty()) {
                            emptyList<MemoModel>().toMutableList()
                        } else {
                            work1.await()
                        }

                        recyclerSearch.adapter?.notifyDataSetChanged()
                    }
                    mainActivity.hideSoftInput()
                    true
                } else {
                    false
                }
            }
        }
    }

    // 항목을 눌러 메모 보는 화면으로 이동하는 처리
    fun showMemoData(position: Int) {
        // 비밀 메모인지 확인한다.
        if (searchMemoList[position].memoIsSecret) {
            val builder = MaterialAlertDialogBuilder(mainActivity)
            builder.setTitle("비밀번호 입력")

            val dialogMemoPasswordBinding = DialogMemoPasswordBinding.inflate(layoutInflater)
            builder.setView(dialogMemoPasswordBinding.root)

            builder.setNegativeButton("취소", null)
            builder.setPositiveButton("확인") { dialogInterface: DialogInterface, i: Int ->
                // 사용자가 입력한 비밀번호를 가져온다.
                val inputPassword =
                    dialogMemoPasswordBinding.textFieldDialogMemoPassword.editText?.text.toString()
                // 입력한 비밀번호를 제대로 입력했다면
                if (inputPassword == searchMemoList[position].memoPassword) {
                    // 메모 번호를 전달한다.
                    val dataBundle = Bundle()
                    dataBundle.putInt("memoIdx", searchMemoList[position].memoIdx)
                    mainActivity.replaceFragment(
                        FragmentName.READ_MEMO_FRAGMENT,
                        true,
                        true,
                        dataBundle
                    )
                } else {
                    val snackbar = Snackbar.make(
                        mainActivity.activityMainBinding.root,
                        "비밀번호를 잘못 입력하였습니다",
                        Snackbar.LENGTH_SHORT
                    )
                    snackbar.show()
                }
            }
            builder.show()
        } else {
            // 메모 번호를 전달한다.
            val dataBundle = Bundle()
            dataBundle.putInt("memoIdx", searchMemoList[position].memoIdx)
            mainActivity.replaceFragment(FragmentName.READ_MEMO_FRAGMENT, true, true, dataBundle)
        }
    }

    fun settingRecyclerViewSearch() {
        fragmentSearchMemoBinding.apply {
            recyclerSearch.adapter = RecyclerViewSearchAdapter()
            recyclerSearch.layoutManager = LinearLayoutManager(mainActivity)
            val deco = DividerItemDecoration(mainActivity, DividerItemDecoration.VERTICAL)
            recyclerSearch.addItemDecoration(deco)
        }
    }

    inner class RecyclerViewSearchAdapter() :
        RecyclerView.Adapter<RecyclerViewSearchAdapter.ViewHolderSearch>() {
        inner class ViewHolderSearch(var rowMemoBinding: RowMemoBinding) :
            RecyclerView.ViewHolder(rowMemoBinding.root) {

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderSearch {
            val rowMemoBinding = RowMemoBinding.inflate(layoutInflater, parent, false)
            val viewHolderSearch = ViewHolderSearch(rowMemoBinding)

            rowMemoBinding.root.setOnClickListener {
                if (searchMemoList.isNotEmpty()) {
                    // mainActivity.replaceFragment(FragmentName.READ_MEMO_FRAGMENT, true, true, null)
                    // 항목을 눌러 메모 보는 화면으로 이동하는 처리
                    showMemoData(viewHolderSearch.adapterPosition)
                }
            }

            // 즐겨찾기 버튼 처리
            rowMemoBinding.buttonRowFavorite.setOnClickListener {
                // 사용자가 선택한 항목 번째 객체를 가져온다.
                val memoModel = searchMemoList[viewHolderSearch.adapterPosition]
                // 즐겨찾기 값을 반대값으로 넣어준다.
                memoModel.memoIsFavorite = !memoModel.memoIsFavorite
                // 즐겨찾기 값을 수정한다.
                CoroutineScope(Dispatchers.Main).launch {
                    val work1 = async(Dispatchers.IO) {
                        MemoRepository.updateMemoFavorite(
                            mainActivity,
                            memoModel.memoIdx,
                            memoModel.memoIsFavorite
                        )
                    }
                    work1.join()

                    // 즐겨찾기 라면...
                    if (arguments?.getString("MemoName") == MemoListName.MEMO_NAME_FAVORITE.str) {
                        // 현재 번째 객체를 제거한다.
                        searchMemoList.removeAt(viewHolderSearch.adapterPosition)
                        fragmentSearchMemoBinding.recyclerSearch.adapter?.notifyItemRemoved(
                            viewHolderSearch.adapterPosition
                        )
                    } else {
                        val a1 = rowMemoBinding.buttonRowFavorite as MaterialButton
                        if (memoModel.memoIsFavorite) {
                            a1.setIconResource(R.drawable.star_full_24px)
                        } else {
                            a1.setIconResource(R.drawable.star_24px)
                        }
                    }
                }
            }

            return viewHolderSearch
        }

        override fun getItemCount(): Int {
            if (searchMemoList.isEmpty()) {
                return 1
            } else {
                return searchMemoList.size
            }
        }

        override fun onBindViewHolder(holder: ViewHolderSearch, position: Int) {
            if (searchMemoList.isEmpty()) {
                holder.rowMemoBinding.textViewRowTitle.text = "검색된 메모가 없습니다"
                holder.rowMemoBinding.textViewRowTitle.setTextColor(Color.BLACK)
                holder.rowMemoBinding.buttonRowFavorite.visibility = View.INVISIBLE
            } else {
                if (searchMemoList[position].memoIsSecret) {
                    holder.rowMemoBinding.textViewRowTitle.text = "비밀 메모 입니다"
                    holder.rowMemoBinding.textViewRowTitle.setTextColor(Color.LTGRAY)
                } else {
                    holder.rowMemoBinding.textViewRowTitle.text = searchMemoList[position].memoTitle
                    holder.rowMemoBinding.textViewRowTitle.setTextColor(Color.BLACK)
                }

                holder.rowMemoBinding.buttonRowFavorite.visibility = View.VISIBLE

                val a1 = holder.rowMemoBinding.buttonRowFavorite as MaterialButton
                if (searchMemoList[position].memoIsFavorite) {
                    a1.setIconResource(R.drawable.star_full_24px)
                } else {
                    a1.setIconResource(R.drawable.star_24px)
                }
            }
        }
    }
}