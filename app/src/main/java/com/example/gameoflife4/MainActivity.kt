package com.example.gameoflife4

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.divyanshu.colorseekbar.ColorSeekBar
import java.util.*

private var liveColor: Int = Color.WHITE

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var turn: TextView
    private var items: MutableList<Cell> = mutableListOf<Cell>()
    private var newItems: MutableList<Cell> = mutableListOf<Cell>()
    private lateinit var player: String
    private var cellsHorizontal = 20
    private var totalCells = cellsHorizontal*cellsHorizontal

    private lateinit var generationButton: Button
    private lateinit var mainHandler: Handler
    private var paused: Boolean = true
    private lateinit var cellColorBar: ColorSeekBar


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        for (i in 0 until totalCells) {
            items.add(Cell())
            newItems.add(Cell())
        }

        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = GridLayoutManager(this, cellsHorizontal)
        recyclerView.adapter = CellAdapter(items) { position -> onListItemClick(position) }

        generationButton = findViewById(R.id.generationButton)

        mainHandler = Handler(Looper.getMainLooper())

        generationButton.setOnClickListener {
            pauseOrResume()
        }

        cellColorBar = findViewById(R.id.cell_color_bar)
        cellColorBar.setOnColorChangeListener(object: ColorSeekBar.OnColorChangeListener{
            override fun onColorChangeListener(color: Int) {
                liveColor = color
                recyclerView.adapter?.notifyDataSetChanged()
            }
        })
    }

    // Help from https://newbedev.com/kotlin-call-a-function-every-second
    private val updateGenerationTask = object : Runnable {
        override fun run() {
            advanceGeneration()
            mainHandler.postDelayed(this, 10)
        }
    }

    private fun pauseOrResume() {
        if(paused) {
            Toast.makeText(this, "Unpaused generations", Toast.LENGTH_SHORT).show()
            generationButton.setText("Pause")
            mainHandler.post(updateGenerationTask)
        }
        else {
            Toast.makeText(this, "Paused generations", Toast.LENGTH_SHORT).show()
            generationButton.setText("Continue")
            mainHandler.removeCallbacks(updateGenerationTask)
        }
        paused = !paused
    }

    private fun clearNewItems() {
        for (i in 0 until totalCells) {
            newItems[i].alive = false
        }
    }

    private fun advanceGeneration() {
        for (i in 0 until totalCells) {
            var numNeighbors = 0
            numNeighbors += if (items[(i - cellsHorizontal - 1).mod(totalCells)].alive) 1 else 0
            numNeighbors += if (items[(i - cellsHorizontal).mod(totalCells)].alive) 1 else 0
            numNeighbors += if (items[(i - cellsHorizontal + 1).mod(totalCells)].alive) 1 else 0
            numNeighbors += if (items[(i - 1).mod(totalCells)].alive) 1 else 0
            numNeighbors += if (items[(i + 1).mod(totalCells)].alive) 1 else 0
            numNeighbors += if (items[(i + cellsHorizontal - 1).mod(totalCells)].alive) 1 else 0
            numNeighbors += if (items[(i + cellsHorizontal).mod(totalCells)].alive) 1 else 0
            numNeighbors += if (items[(i + cellsHorizontal + 1).mod(totalCells)].alive) 1 else 0
            if (items[i].alive && numNeighbors < 2) {
                newItems[i].alive = false
            }
            if (items[i].alive && (numNeighbors == 2 || numNeighbors == 3)) {
                newItems[i].alive = true
            }
            if (items[i].alive && numNeighbors > 3) {
                newItems[i].alive = false
            }
            if (!items[i].alive && numNeighbors == 3) {
                newItems[i].alive = true
            }
        }

        for (i in 0 until totalCells) {
            items[i].alive = newItems[i].alive
        }
        recyclerView.adapter?.notifyDataSetChanged()
        clearNewItems()
    }

    private fun onListItemClick(position: Int) {
        items[position].alive = !items[position].alive
        recyclerView.adapter?.notifyItemChanged(position)
    }

    private class CellViewHolder(cellView: View,
                                 private val onItemClicked: (position: Int) -> Unit
    ): RecyclerView.ViewHolder(cellView), View.OnClickListener {
        val button: ImageButton

        init {
            button = cellView.findViewById(R.id.cell_button)
            button.setOnClickListener(this)
        }

        fun display(alive: Boolean) {
            if(alive) {
                button.setBackgroundColor(liveColor)
            }
            else {
                button.setBackgroundColor(Color.rgb(0, 0, 0))
            }
            /*button.animate().apply {
                rotationBy(360f)
            }.start()*/
        }

        override fun onClick(v: View) {
            val position = adapterPosition
            onItemClicked(position)
        }

    }

    private class CellAdapter(private var items: MutableList<Cell>,
                              private val onItemClicked: (position: Int) -> Unit
    ) : RecyclerView.Adapter<CellViewHolder>() {
        final val CELLS_WIDTH: Int = 20
        final val NUM_CELLS: Int = CELLS_WIDTH * CELLS_WIDTH

        var selectedIndex = -1
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CellViewHolder {
            val inflater: LayoutInflater = LayoutInflater.from(parent.context)
            val cellView: View = inflater.inflate(R.layout.cell, parent, false)
            return CellViewHolder(cellView, onItemClicked)

        }

        override fun onBindViewHolder(holder: CellViewHolder, position: Int) {
            val item = items[position]
            holder.display(item.alive)
        }

        override fun getItemCount(): Int {
            return NUM_CELLS
        }

    }
}