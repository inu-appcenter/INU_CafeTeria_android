package org.inu.cafeteria.feature.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.login_fragment.view.*
import org.inu.cafeteria.R
import org.inu.cafeteria.common.base.BaseFragment
import org.inu.cafeteria.common.extension.getViewModel
import org.inu.cafeteria.common.extension.handleRetrofitException
import timber.log.Timber

class LoginFragment : BaseFragment() {

    private lateinit var viewModel: LoginViewModel

    private val onLoginButtonClick = { view: View ->
        val id = view.student_id.text.toString()
        val pw = view.password.text.toString()
        val auto = view.autologin.isChecked

        with(viewModel) {
            tryLoginWithIdAndPw(
                id = id,
                pw = pw,
                auto = auto,
                onSuccess = {
                    saveLoginResult(it, id)
                    showMain(this@LoginFragment)
                },
                onFail = ::handleLoginFailure
            )
        }
    }

    init {
        failables += this
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = getViewModel {
            tryAutoLogin(
                onPass = {
                    Timber.i("No token available")
                },
                onSuccess = {
                    Timber.i("Auto login succeeded.")
                    showMain(this@LoginFragment)
                },
                onFail = ::handleAutoLoginFailure
             )
        }
        failables += viewModel.failables
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.login_fragment, container, false).apply {
            initializeView(this)
        }
    }

    private fun initializeView(view: View) {
        with(view.login) {
            setOnClickListener { onLoginButtonClick(view) }
        }

        with(view.no_user_login) {
            setOnClickListener {
                // Bypass the login.
                viewModel.showMain(this@LoginFragment)
            }
        }
    }
}