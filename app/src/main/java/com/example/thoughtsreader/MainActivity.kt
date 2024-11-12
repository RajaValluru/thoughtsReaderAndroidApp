package com.example.thoughtsreader

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.ImageView

import android.widget.TextView

import androidx.activity.ComponentActivity

import androidx.activity.enableEdgeToEdge
import com.example.thoughtsreader.MainActivity.Cards.CardPlayer
import com.example.thoughtsreader.MainActivity.Dice.DicePlayer

import java.net.DatagramPacket
import java.net.DatagramSocket

import java.util.LinkedList


class MainActivity : ComponentActivity() {
    enum class Game {
        CARDS,
        DICE,
        POKER,
        NOTHING
    }
    class Cards {
        class CardPlayer {
            lateinit var playerName: String
            var playerNumber: Int = -1
            var triesLeft = 6
            var cards: Array<Int> = Array(5) {0}
        }
        var players = HashMap<String, CardPlayer>()
        var cardsThrown = ArrayList<String>()
    }

    class Dice {
        class DicePlayer {
            lateinit var playerName: String
            var playerNumber: Int = -1
            var dice: Array<Int> = Array(5) {0}
        }

        var players = HashMap<String, DicePlayer>()
        var totalDice: Array<String> = Array(6) {""}
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContentView(R.layout.activity_main)

        imageViews = arrayOf(
            findViewById(R.id.imageView1),
            findViewById(R.id.imageView2),
            findViewById(R.id.imageView3),
            findViewById(R.id.imageView4),
            findViewById(R.id.imageView5),
            findViewById(R.id.imageView6)
        )
        textViews = arrayOf(
            findViewById(R.id.textView1),
            findViewById(R.id.textView2),
            findViewById(R.id.textView3),
            findViewById(R.id.textView4),
            findViewById(R.id.textView5),
            findViewById(R.id.textView6)
        )

        val resetButton: Button = findViewById(R.id.resetButton)
        resetButton.setOnClickListener {
            currentDiceGame = Dice()
            currentCardGame = Cards()
            clearDisplay()
        }
        showCards = findViewById(R.id.showCards)
        showCards.setOnClickListener {
            currentPlayedGame = Game.CARDS

        }

        showDice = findViewById(R.id.showDice)
        showDice.setOnClickListener {
            currentPlayedGame = Game.DICE

        }
        identifyAllImages()
        this.textViewLogss = findViewById(R.id.textViewLogs)
        connectToServer()

        Thread {
            handler.post(updateRunnable)
        }.start()

    }


    private fun connectToServer() {
        Thread {
            try {
                val networkAvailability = isNetworkAvailable()

                val socket = DatagramSocket(serverPort)
                val receivedData = ByteArray(1024)
                currentCardGame = Cards()
                currentDiceGame = Dice()
                while (true) {

                    val receivedPacket = DatagramPacket(receivedData, receivedData.size)

                    socket.receive(receivedPacket)
                    val logMessage = String(receivedPacket.data, 0, receivedPacket.length)


                    handler.post {
                            addLogLine(logMessage)
                            updateLogDisplay()

                    }
                    processTheLog(logMessage);

                }
            } catch (e: Exception) {
                Log.d("LogViewer", "Eror", e)

                addLogLine("Error ${e.message}")
                updateLogDisplay()
            }
        }.start()
    }

    private fun addLogLine(logMessage: String) {
        logLines.addLast(logMessage)
        if (logLines.size > maxLines) {
            logLines.removeFirst()
        }
    }

    private fun updateLogDisplay() {
        var bosa =  StringBuilder()

        logLines.forEach { bligga ->
            bosa.append(bligga + "\n")
        }

        Log.d("LogViewer", "Updated the display with message $bosa")

        textViewLogss.text = bosa.toString()
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            else -> false
        }

    }

    private fun processTheLog(message: String) {
        val stringer = ArrayList(message.split('|'))
        Log.d("LogViewer", message)
        when (stringer[0]) {
            "CardsThrown" -> {
                currentCardGame.cardsThrown.clear()
                for (i in 1..<stringer.size) {
                    currentCardGame.cardsThrown.add(stringer[i])
                }
                currentCardGame.cardsThrown.sort()
            }
            "CardsHaved" -> {
                if (stringer.size>5) {
                    val player = currentCardGame.players.getOrDefault(stringer[1], CardPlayer())
                    if (player.playerNumber == -1)
                        player.playerNumber = currentCardGame.players.count()
                    player.playerName = stringer[1]
                    for (i in 0..4) {
                        when(stringer[i+2]) {
                            "K" -> player.cards[i] = R.mipmap.king
                            "Q" -> player.cards[i] = R.mipmap.queen
                            "J" -> player.cards[i] = R.mipmap.joker
                            "D" -> player.cards[i] = R.mipmap.devil
                            "A" -> player.cards[i] = R.mipmap.ace
                            else -> player.cards[i] = R.mipmap.empty
                        }
                    }
                    player.cards.sort()
                    currentCardGame.players[stringer[1]] = player
                }
            }
            "Tries" -> {
                val player = currentCardGame.players.getOrDefault(stringer[1], CardPlayer())
                if (player.playerNumber == -1)
                    player.playerNumber = currentCardGame.players.count()
                player.playerName = stringer[1]
                if(stringer[2]=="GG")
                    player.triesLeft = 1
                else
                    player.triesLeft = stringer[2].toInt()
                currentCardGame.players[stringer[1]] = player
            }

            "DiceCount" -> {
                if (stringer.size>5) {
                    val player = currentDiceGame.players.getOrDefault(stringer[1], DicePlayer())
                    if (player.playerNumber == -1)
                        player.playerNumber = currentDiceGame.players.count()
                    player.playerName = stringer[1]
                    for (i in 0..4) {
                        when (stringer[i + 2]) {
                            "1" -> player.dice[i] = R.mipmap.one
                            "2" -> player.dice[i] = R.mipmap.two
                            "3" -> player.dice[i] = R.mipmap.three
                            "4" -> player.dice[i] = R.mipmap.four
                            "5" -> player.dice[i] = R.mipmap.five
                            "6" -> player.dice[i] = R.mipmap.six
                            else -> player.dice[i] = R.mipmap.empty
                        }
                    }
                    player.dice.sort()
                    currentDiceGame.players[player.playerName] = player
                }
            }
            "TotalCount" -> {
                currentDiceGame.totalDice[stringer[1].toInt()-1] = stringer[2]
            }
            else -> {
                Log.d("LogViewer", "Who the word knows about this event $message")
            }
        }
    }

    private val updateRunnable = object : Runnable {
        override fun run() {
            if (isUpdating) {
                // Update the TableLayout
                when (currentPlayedGame) {
                    Game.CARDS -> updateDisplayCardGame()
                    Game.DICE -> updateDisplayDiceGame()
                    Game.POKER -> clearDisplay()
                    Game.NOTHING -> clearDisplay()
                }

                // Schedule the next update in 2 seconds
                handler.postDelayed(this, 2000)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun clearDisplay() {
        runOnUiThread {
            currentPlayedGame = Game.NOTHING
            playerTexts.forEach { view -> view.text = "" }
            playerImages.forEach { imageList -> run {
                    imageList.forEach { image -> image.setImageResource(R.mipmap.empty) }
                }
            }
            for (i in 0..5) {
                imageViews[i].setImageResource(R.mipmap.empty)
                textViews[i].text = ""
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateDisplayCardGame() {
        // Clear previous rows if any
        runOnUiThread {
            try {
                //setSampleCardGame()

                var cardNumba = 0
                currentCardGame.cardsThrown.forEach { card ->
                    run {
                        when (card) {
                            "K" -> imageViews[cardNumba].setImageResource(R.mipmap.king)
                            "Q" -> imageViews[cardNumba].setImageResource(R.mipmap.queen)
                            "J" -> imageViews[cardNumba].setImageResource(R.mipmap.joker)
                            "A" -> imageViews[cardNumba].setImageResource(R.mipmap.ace)
                            "D" -> imageViews[cardNumba].setImageResource(R.mipmap.devil)
                            else -> imageViews[cardNumba].setImageResource(R.mipmap.empty)
                        }
                        cardNumba++;
                    }
                }

                while (cardNumba<=5) {
                    imageViews[cardNumba].setImageResource(R.mipmap.empty)
                    cardNumba++
                }


                //Go through players
                currentCardGame.players.forEach { player -> run{
                        playerTexts[player.value.playerNumber].text = "${player.key.substring(0,minOf(player.key.length, 8))} - ${player.value.triesLeft}"
                        for (i in 0..4)
                            playerImages[player.value.playerNumber][i].setImageResource(player.value.cards[i])
                    }
                }


            } catch (e: Exception) {
                Log.d("LogViewer", "BAN ERRORs ", e)

                addLogLine("Error ${e.message}")
                updateLogDisplay()
            }
        }

    }

    @SuppressLint("SetTextI18n")
    private fun updateDisplayDiceGame() {
        runOnUiThread {
            try {

                //setSampleDiceGame()
                imageViews[0].setImageResource(R.mipmap.one)
                imageViews[1].setImageResource(R.mipmap.two)
                imageViews[2].setImageResource(R.mipmap.three)
                imageViews[3].setImageResource(R.mipmap.four)
                imageViews[4].setImageResource(R.mipmap.five)
                imageViews[5].setImageResource(R.mipmap.six)

                for(i in 0..5) {
                    textViews[i].text = "x ${currentDiceGame.totalDice[i]}"
                }

                currentDiceGame.players.forEach { player -> run{
                    playerTexts[player.value.playerNumber].text = player.key.substring(0,minOf(player.key.length, 8))
                    for (i in 0..4)
                        playerImages[player.value.playerNumber][i].setImageResource(player.value.dice[i])
                    }
                }

            } catch (e: Exception) {
                Log.d("LogViewer", "BAN ERRORs ", e)

                addLogLine("Error ${e.message}")
                updateLogDisplay()
            }
        }
    }

    private fun setSampleCardGame() {
        currentCardGame = Cards()

        currentCardGame.cardsThrown.add("K")
        currentCardGame.cardsThrown.add("Q")
        currentCardGame.cardsThrown.add("A")

        currentCardGame.players["Raja"] = CardPlayer()
        currentCardGame.players["Raja"]?.playerNumber = currentCardGame.players.count() - 1
        currentCardGame.players["Raja"]?.playerName = "Raja"
        currentCardGame.players["Raja"]?.triesLeft = 5
        currentCardGame.players["Raja"]?.cards = arrayOf(R.mipmap.ace, R.mipmap.king, R.mipmap.ace, R.mipmap.ace, R.mipmap.ace)

        currentCardGame.players["p2"] = CardPlayer()
        currentCardGame.players["p2"]?.playerNumber = currentCardGame.players.count() - 1
        currentCardGame.players["p2"]?.playerName = "p2"
        currentCardGame.players["p2"]?.triesLeft = 6
        currentCardGame.players["p2"]?.cards = arrayOf(R.mipmap.ace, R.mipmap.queen, R.mipmap.queen, R.mipmap.joker, R.mipmap.devil)

        currentCardGame.players["p3"] = CardPlayer()
        currentCardGame.players["p3"]?.playerNumber = currentCardGame.players.count() - 1
        currentCardGame.players["p3"]?.playerName = "p3"
        currentCardGame.players["p3"]?.triesLeft = 3
        currentCardGame.players["p3"]?.cards = arrayOf(R.mipmap.queen, R.mipmap.king, R.mipmap.devil, R.mipmap.ace, R.mipmap.ace)

        currentCardGame.players["p4"] = CardPlayer()
        currentCardGame.players["p4"]?.playerNumber = currentCardGame.players.count() - 1
        currentCardGame.players["p4"]?.playerName = "p4"
        currentCardGame.players["p4"]?.triesLeft = 1
        currentCardGame.players["p4"]?.cards = arrayOf(R.mipmap.ace, R.mipmap.devil, R.mipmap.ace, R.mipmap.ace, R.mipmap.joker)
    }

    private fun setSampleDiceGame() {
        currentDiceGame = Dice()
        currentDiceGame.totalDice[0] = "1"
        currentDiceGame.totalDice[1] = "5"
        currentDiceGame.totalDice[2] = "0"
        currentDiceGame.totalDice[3] = "8"
        currentDiceGame.totalDice[4] = "3"
        currentDiceGame.totalDice[5] = "7"
        currentDiceGame.players["Raja"] = DicePlayer()
        currentDiceGame.players["Raja"]?.playerName = "Raja"
        currentDiceGame.players["Raja"]?.playerNumber = currentDiceGame.players.count() - 1
        currentDiceGame.players["Raja"]?.dice = arrayOf(R.mipmap.five, R.mipmap.two, R.mipmap.three, R.mipmap.four, R.mipmap.five)
        currentDiceGame.players["p2"] = DicePlayer()
        currentDiceGame.players["p2"]?.playerName = "p2"
        currentDiceGame.players["p2"]?.dice = arrayOf(R.mipmap.one, R.mipmap.four, R.mipmap.three, R.mipmap.four, R.mipmap.one)
        currentDiceGame.players["p2"]?.playerNumber = currentDiceGame.players.count() - 1
        currentDiceGame.players["p3"] = DicePlayer()
        currentDiceGame.players["p3"]?.playerName = "p3"
        currentDiceGame.players["p3"]?.dice = arrayOf(R.mipmap.three, R.mipmap.two, R.mipmap.three, R.mipmap.four, R.mipmap.five)
        currentDiceGame.players["p3"]?.playerNumber = currentDiceGame.players.count() - 1
        currentDiceGame.players["p4"] = DicePlayer()
        currentDiceGame.players["p4"]?.playerName = "p4"
        currentDiceGame.players["p4"]?.dice = arrayOf(R.mipmap.one, R.mipmap.two, R.mipmap.two, R.mipmap.one, R.mipmap.six)
        currentDiceGame.players["p4"]?.playerNumber = currentDiceGame.players.count() - 1
    }

    private fun identifyAllImages() {
        playerTexts = arrayOf(
            findViewById(R.id.playerName1),
            findViewById(R.id.playerName2),
            findViewById(R.id.playerName3),
            findViewById(R.id.playerName4)
        )
        playerTexts[0].textSize = 17F
        playerTexts[1].textSize = 17F
        playerTexts[2].textSize = 17F
        playerTexts[3].textSize = 17F

        playerImages = arrayOf(
            arrayOf(
                findViewById(R.id.playerImage1_1),
                findViewById(R.id.playerImage1_2),
                findViewById(R.id.playerImage1_3),
                findViewById(R.id.playerImage1_4),
                findViewById(R.id.playerImage1_5),
            ),
            arrayOf(
                findViewById(R.id.playerImage2_1),
                findViewById(R.id.playerImage2_2),
                findViewById(R.id.playerImage2_3),
                findViewById(R.id.playerImage2_4),
                findViewById(R.id.playerImage2_5),
            ),
            arrayOf(
                findViewById(R.id.playerImage3_1),
                findViewById(R.id.playerImage3_2),
                findViewById(R.id.playerImage3_3),
                findViewById(R.id.playerImage3_4),
                findViewById(R.id.playerImage3_5),
            ),
            arrayOf(
                findViewById(R.id.playerImage4_1),
                findViewById(R.id.playerImage4_2),
                findViewById(R.id.playerImage4_3),
                findViewById(R.id.playerImage4_4),
                findViewById(R.id.playerImage4_5),
            )
        )
    }


    override fun onDestroy() {
        super.onDestroy()
        isUpdating = false
        handler.removeCallbacks(updateRunnable)
    }


    private lateinit var showDice: Button
    private lateinit var showCards: Button
    private lateinit var textViewLogss: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val logLines: LinkedList<String> = LinkedList()
    private val maxLines = 7
    private val serverPort = 9999
    private lateinit var currentCardGame: Cards
    private lateinit var currentDiceGame: Dice
    private var currentPlayedGame = Game.NOTHING
    private var isUpdating = true;
    private lateinit var playerTexts: Array<TextView>
    private lateinit var playerImages: Array<Array<ImageView>>

    private lateinit var imageViews: Array<ImageView>
    private lateinit var textViews: Array<TextView>

}
