package de.hdm.smart_penguins.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import de.hdm.smart_penguins.data.model.NodeList

class HomeViewModel : ViewModel() {



    private val _text = MutableLiveData<String>().apply {
        value = "This is home Fragment"
    }
    val text: LiveData<String> = _text
    val nodeList = MutableLiveData<NodeList>()

}