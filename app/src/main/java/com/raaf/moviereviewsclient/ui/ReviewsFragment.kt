package com.raaf.moviereviewsclient.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.raaf.moviereviewsclient.App
import com.raaf.moviereviewsclient.R
import com.raaf.moviereviewsclient.ui.adapters.ReviewsAdapter
import com.raaf.moviereviewsclient.ui.adapters.ReviewsLoaderStateAdapter
import com.raaf.moviereviewsclient.ui.extensions.lazyViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ReviewsFragment : Fragment() {

    private lateinit var startProgress: ProgressBar
    private lateinit var reviewsRV: RecyclerView
    val reviewsVM: ReviewsViewModel by lazyViewModel {
        App.reviewsComponent.reviewsViewModel().create(it)
    }
    private var layoutManager: LinearLayoutManager? = null
    private var reviewsAdapter: ReviewsAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_reviews, container, false)
        startProgress = view.findViewById(R.id.start_progress_bar)
        reviewsRV = view.findViewById(R.id.reviews_recycler_view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        layoutManager = LinearLayoutManager(this.context, LinearLayoutManager.VERTICAL, false)
        reviewsRV.layoutManager = layoutManager
        reviewsAdapter = ReviewsAdapter(layoutManager!!, reviewsRV)

//        Processing states of data in recycler
//        We can wrap recycler in SwipeRefreshLayout instead of using loader state adapters
        reviewsRV.adapter = reviewsAdapter!!.withLoadStateHeaderAndFooter(
            ReviewsLoaderStateAdapter { reviewsAdapter!!.retry() },
            ReviewsLoaderStateAdapter { reviewsAdapter!!.retry() }
        )

        reviewsAdapter!!.addLoadStateListener {
            if (it.refresh == LoadState.Loading) {
                reviewsRV.visibility = View.GONE
                startProgress.visibility = View.VISIBLE
            } else {
                reviewsRV.visibility = View.VISIBLE
                startProgress.visibility = View.GONE
            }
        }

        setSavedPositionToAdapter(reviewsVM.getSavedPosition())

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                reviewsVM.reviews.collectLatest(reviewsAdapter!!::submitData)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        savingPagingState()
        layoutManager = null
        reviewsAdapter = null
    }

    private fun setSavedPositionToAdapter(position: Int?) {
        if (position != null) reviewsAdapter?.setSavedPosition(position)
    }

    //    Passing value to be saved in SavedStateHandle
    private fun savingPagingState() {
        val position = layoutManager?.findLastVisibleItemPosition() ?: 0
        if (position > 0) reviewsVM.saveOffset(position)
    }
}