package com.net.soc.networksocketport

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.net.soc.networksocketport.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.Socket

@DelicateCoroutinesApi
class MainActivity : AppCompatActivity() {

    val TAG = "MainActivity"
    private lateinit var main_binding: ActivityMainBinding
    private lateinit var my_socket: Socket

    private lateinit var my_dataOutputStream: DataOutputStream
    private lateinit var my_buffer_reader: BufferedReader

    private var matriculation_input_num : String? = null

    private var response_msg : String? = null

    private var numbersListFromInput = mutableListOf<Int>()

    companion object{
        var my_sz = 1e5.toInt()
        var my_isPrime = BooleanArray(my_sz + 1)
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        main_binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(main_binding.root)


        main_binding.sendButton.setOnClickListener {

            matriculation_input_num = main_binding.eingabefeldImmatrikulation.text.toString()

            if (TextUtils.isEmpty(matriculation_input_num)){

                main_binding.serverantwort.text = ""
                main_binding.berechnungsergebnis.text = ""
                main_binding.immatrikulationEingabeLayout.error = "Matrikelnummer fehlt"
                return@setOnClickListener
            }

            // send matriculation number from here //
            main_binding.immatrikulationEingabeLayout.error = null

            GlobalScope.launch(Dispatchers.IO) {

                getResFromTcpConncetion()

            }
        }

        main_binding.berechnung.setOnClickListener {


            if (TextUtils.isEmpty(matriculation_input_num)){

                main_binding.serverantwort.text = ""
                main_binding.berechnungsergebnis.text = ""
                main_binding.immatrikulationEingabeLayout.error = "Matrikelnummer fehlt"
                return@setOnClickListener
            }

            val myinputInt = matriculation_input_num?.toInt()
            val modulus = myinputInt!! % 7

            Log.d(TAG, "onCreate: MODULUS VAL IS: $modulus")
            
            doTaskAccordingToModulusResult(modulus)


        }
    }

    @SuppressLint("SetTextI18n")
    private fun doTaskAccordingToModulusResult(modulusRes : Int){
        
        when(modulusRes){

            6 -> {

                Log.d(TAG, "doTaskAccordingToModulusResult: 6 executed")

                GlobalScope.launch(Dispatchers.Default) {

                    val inputInList = getNumberListFromInput(matriculation_input_num!!)
                    removePrimesFromArray(inputInList.toTypedArray(),inputInList.size)

                }

            }
        }
    }


    // case 6 //

     private suspend fun getNumberListFromInput(input: String) : MutableList<Int>{

        val mutableListNumbers = mutableListOf<Int>()

        for (i in input.indices) {
            mutableListNumbers.add(Character.toString(input.get(i)).toInt())
        }


       return mutableListNumbers
    }

    private suspend fun sievePrime() {
        for (i in 0 until my_sz + 1) my_isPrime[i] = true
        my_isPrime[1] = false
        my_isPrime[0] = my_isPrime.get(1)
        var i = 2
        while (i * i <= my_sz) {
            if (my_isPrime.get(i)) {
                var j = i * i
                while (j < my_sz) {
                    my_isPrime[j] = false
                    j += i
                }
            }
            i++
        }
    }

    private suspend fun removePrimesFromArray(arr: Array<Int>, len: Int) {
        // Generate primes
        var len = len
        sievePrime()

        // Traverse the array
        var i = 0
        while (i < len) {


            // If the current element is prime
            if (my_isPrime.get(arr[i])) {

                // Shift all the elements on the
                // right of it to the left
                for (j in i until len - 1) {
                    arr[j] = arr[j + 1]
                }

                // Decrease the loop counter by 1
                // to check the shifted element
                i--

                // Decrease the length
                len--
            }
            i++
        }

//        // Print the updated array
        printMyNonPrimeArray(arr, len)
    }


    // Function to print the elements of the array
    fun printMyNonPrimeArray(arr: Array<Int>, len: Int) {

        GlobalScope.launch(Dispatchers.Main) {

            val nonprimeNumbers  = mutableListOf<Int>()

            var str : String = ""

            for (i in 0 until len) {
                str = str + arr[i] + " "
                nonprimeNumbers.add(arr[i])

            }

            val sortedOddList = nonprimeNumbers.sorted()

            main_binding.berechnungsergebnis.text = "Modulus 6 -> Result is: ${sortedOddList.toString()}"

        }

    }
    private suspend fun getResFromTcpConncetion() {

       try {

           my_socket  = withContext(Dispatchers.IO) {
               Socket("se2-isys.aau.at", 53212)
           }

           my_socket.use { socket ->

               my_dataOutputStream = DataOutputStream(socket.getOutputStream())
               my_buffer_reader = BufferedReader(InputStreamReader(socket.getInputStream()))

               my_dataOutputStream.writeBytes(matriculation_input_num + '\n')

               response_msg = my_buffer_reader.readLine()

               Log.d(TAG, "Response is: ${response_msg.toString()}")

               withContext(Dispatchers.Main){

                   main_binding.serverantwort.text = response_msg
               }

           }


        }catch (ex :IOException){

            ex.stackTrace

        }

    }
}