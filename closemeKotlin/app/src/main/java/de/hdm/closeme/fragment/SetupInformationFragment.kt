package de.hdm.closeme.fragment


import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.hdm.closeme.MainActivity

import de.hdm.closeme.R
import de.hdm.closeme.constant.Constants
import de.hdm.closeme.constant.Constants.Companion.ARGUMENT_SETUP_FIRST_PAGE
import kotlinx.android.synthetic.main.fragment_setup_information.*


/**
 * A simple [Fragment] subclass.
 *
 */
class SetupInformationFragment : Fragment() {

    private var type = Constants.TAG_EMPTY

    companion object {

        @JvmStatic
        fun newInstance(type: String) = SetupInformationFragment().apply {
            arguments = Bundle().apply {
                putString(Constants.ARGUMENT_SERVICE_STATE, type)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_setup_information, container, false)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        arguments?.getString(Constants.ARGUMENT_SERVICE_STATE)?.let { this.type = it }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        when (type) {
            ARGUMENT_SETUP_FIRST_PAGE -> {
                firstInstruction.visibility = View.VISIBLE
                impressum.visibility = View.GONE
                setupInformationText.text =
                        getString(R.string.setup_information_next_step)
                finishButton.visibility = View.GONE

            }
            else -> {
                firstInstruction.visibility = View.GONE
                impressum.visibility = View.VISIBLE
                setupInformationText.text = getString(R.string.setup_information_finish)
                finishButton.visibility = View.VISIBLE
            }
        }
        finishButton.setOnClickListener {
            ((activity) as MainActivity).discardSession()
        }
    }
}
