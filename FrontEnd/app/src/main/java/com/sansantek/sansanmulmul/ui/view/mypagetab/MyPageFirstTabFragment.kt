package com.sansantek.sansanmulmul.ui.view.mypagetab

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.sansantek.sansanmulmul.R
import com.sansantek.sansanmulmul.config.BaseFragment
import com.sansantek.sansanmulmul.data.model.Mountain
import com.sansantek.sansanmulmul.data.model.MountainHistory
import com.sansantek.sansanmulmul.databinding.FragmentMyPageFirstTabBinding
import com.sansantek.sansanmulmul.ui.adapter.MyPageFirstTabFavoriteMountainListAdapter
import com.sansantek.sansanmulmul.ui.adapter.MyPageFirstTabHistoryMountainListAdapter
import com.sansantek.sansanmulmul.ui.adapter.itemdecoration.SpaceItemDecoration
import com.sansantek.sansanmulmul.ui.util.RetrofiltUtil.Companion.mountainService
import com.sansantek.sansanmulmul.ui.util.RetrofiltUtil.Companion.recordService
import com.sansantek.sansanmulmul.ui.util.Util.makeHeaderByAccessToken
import com.sansantek.sansanmulmul.ui.view.mountaindetail.MountainDetailFragment
import com.sansantek.sansanmulmul.ui.viewmodel.MainActivityViewModel
import com.sansantek.sansanmulmul.ui.viewmodel.MountainDetailViewModel
import kotlinx.coroutines.launch

private const val TAG = "싸피_MyPageFirstTabFragment"

class MyPageFirstTabFragment : BaseFragment<FragmentMyPageFirstTabBinding>(
  FragmentMyPageFirstTabBinding::bind,
  R.layout.fragment_my_page_first_tab
) {
  private val activityViewModel: MainActivityViewModel by activityViewModels()
  private val mountainDetailViewModel: MountainDetailViewModel by activityViewModels()
  private var historyMountainList = mutableListOf<MountainHistory>()

  private lateinit var favoriteMountainAdapter: MyPageFirstTabFavoriteMountainListAdapter
  private lateinit var historyMountainAdapter: MyPageFirstTabHistoryMountainListAdapter

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    binding.rvFavoriteMountain.apply {
      layoutManager =
        LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false).apply {
          isMeasurementCacheEnabled = false
        }
      favoriteMountainAdapter = MyPageFirstTabFavoriteMountainListAdapter().apply {
        setItemClickListener(object : MyPageFirstTabFavoriteMountainListAdapter.ItemClickListener {
          override fun onClick(mountain: Mountain) {
            mountainDetailViewModel.setMountainID(mountain.mountainId)
            changeFragmentWithPopUpAnimation(MountainDetailFragment())
          }
        })
      }
      loadMyFavoriteMountainList()
      adapter = favoriteMountainAdapter
      addItemDecoration(SpaceItemDecoration(30))
    }
    binding.rvMountainHistory.apply {
      layoutManager =
        LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false).apply {
          isMeasurementCacheEnabled = false
        }
      historyMountainAdapter = MyPageFirstTabHistoryMountainListAdapter()
      historyMountainAdapter.apply {
        submitList(historyMountainList)
        setItemClickListener(object :
          MyPageFirstTabHistoryMountainListAdapter.ItemClickListener {
          override fun onHistoryClick(mountainHistory: MountainHistory) {
            ShowMyPageHistoryDialog(mountainHistory).show(childFragmentManager, null)
          }

        })
      }
      loadMyHikingHistory()
      adapter = historyMountainAdapter
      addItemDecoration(SpaceItemDecoration(30))
    }
  }

  private fun loadMyFavoriteMountainList() {
    activityViewModel.token?.let {
      lifecycleScope.launch {
        val result =
          mountainService.getLikedMountainList(makeHeaderByAccessToken(it.accessToken))
        if (result.isSuccessful) {
          favoriteMountainAdapter.submitList(result.body()!!)
        }
      }
    }
  }

  private fun loadMyHikingHistory() {
    activityViewModel.token?.let {
      lifecycleScope.launch {
        val result =
          recordService.getUserHikingRecord(activityViewModel.user.userId)
        if (result.isSuccessful) {
          Log.d(TAG, "loadMyHikingHistory: 아아아 ${result.body()}")
          historyMountainAdapter.submitList(result.body()!!)
        }
      }
    }
  }
}