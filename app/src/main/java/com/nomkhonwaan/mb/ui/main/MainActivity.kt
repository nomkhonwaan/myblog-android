package com.nomkhonwaan.mb.ui.main

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.nomkhonwaan.mb.R
import com.nomkhonwaan.mb.R.layout.activity_main
import com.nomkhonwaan.mb.ui.recentupdates.RecentUpdatesFragment
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.partial_app_header.*
import kotlinx.android.synthetic.main.partial_app_sidebar.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class MainActivity : AppCompatActivity(), HasSupportFragmentInjector {

    @Inject
    lateinit var supportFragmentInjector: DispatchingAndroidInjector<Fragment>

    private val compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)

        super.onCreate(savedInstanceState)

        setContentView(activity_main)

        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)

        // Force main layout width to the screen width
        main.layoutParams.width = displayMetrics.widthPixels

        // Also force the app layout with to be main layout with plus sidebar width
        app.layoutParams.width = main.layoutParams.width + sidebar.layoutParams.width

        // Create an animation set for the sidebar and popup_overlay when sidebar toggle has been touched
        createToggleSidebarAnimation(displayMetrics)

        // Add list of nav items to the sidebar
        renderSidebarNavigator()

        // Default, will render the recent posts page
        if (savedInstanceState == null) {
            renderRecentUpdates()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        compositeDisposable.dispose()
    }

    override fun supportFragmentInjector(): AndroidInjector<Fragment> {
        return supportFragmentInjector
    }

    /**
     * Create an animation set for animate while toggle sidebar and pop-up overlay.
     *
     * @param {DisplayMetrics} displayMetrics
     */
    private fun createToggleSidebarAnimation(displayMetrics: DisplayMetrics) {
        val observable: Observable<Boolean> = Observable.create { emitter ->
            var isCollapsed = true
            val toggleSidebar = { _: View -> isCollapsed = !isCollapsed; emitter.onNext(isCollapsed) }

            popup_overlay.setOnClickListener(toggleSidebar)
            button_sidebar_open.setOnClickListener(toggleSidebar)
            button_sidebar_close.setOnClickListener(toggleSidebar)
        }

        val disposable: Disposable = observable
                .debounce(200, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
                .subscribe { isCollapsed ->
                    val animatorSet = AnimatorSet()

                    if (!isCollapsed) {
                        popup_overlay.visibility = View.VISIBLE
                    }

                    animatorSet.playTogether(
                            ObjectAnimator.ofFloat(app, "translationX",
                                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, if (isCollapsed) -256f else 0f, displayMetrics)),
                            ObjectAnimator.ofFloat(popup_overlay, "alpha", if (isCollapsed) 0f else 0.16f)
                    )
                    animatorSet.duration = 400
                    animatorSet.addListener(object : Animator.AnimatorListener {
                        override fun onAnimationCancel(animation: Animator?) {}
                        override fun onAnimationRepeat(animation: Animator?) {}
                        override fun onAnimationStart(animation: Animator?) {}
                        override fun onAnimationEnd(animation: Animator?) {
                            if (isCollapsed) {
                                popup_overlay.visibility = View.GONE
                            }
                        }
                    })
                    animatorSet.start()
                }

        disposable.addTo(compositeDisposable)
    }

    /**
     * Apply all sidebar's navigator items to the RecyclerView.
     */
    private fun renderSidebarNavigator() {
        nav.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = NavItemsAdapter(arrayOf(
                    NavItem(resources.getString(R.string.sidebar_home)),
                    NavItem(resources.getString(R.string.sidebar_login_register)),
                    NavItem(resources.getString(R.string.sidebar_web_development)),
                    NavItem(resources.getString(R.string.sidebar_web_design)),
                    NavItem(resources.getString(R.string.sidebar_programming)),
                    NavItem(resources.getString(R.string.sidebar_devops)),
                    NavItem(resources.getString(R.string.sidebar_life_style)),
                    NavItem(resources.getString(R.string.sidebar_how_i_made_this_website))
            ))
        }
    }

    /**
     * Render the recent posts page which will show a list of latest published posts.
     */
    private fun renderRecentUpdates() {
        supportFragmentManager
                .beginTransaction()
                .add(content.id, RecentUpdatesFragment.newInstance())
                .commit()
    }

}
