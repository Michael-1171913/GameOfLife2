package com.example.gameoflife4

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.divyanshu.colorseekbar.ColorSeekBar
import com.google.gson.Gson
import android.R.attr.data
import android.view.*
import com.google.gson.reflect.TypeToken
import java.io.InputStream
import java.io.OutputStream

const val INTENT_MESSAGE: String = "CopyOfMainActivity"

private var liveColor: Int = Color.WHITE
private var backColor: Int = Color.BLACK

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
    private lateinit var backgroundColorBar: ColorSeekBar

    private lateinit var saveButton: Button
    private lateinit var loadButton: Button
    private lateinit var cloneButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)

        if(intent.hasExtra(INTENT_MESSAGE)) {
            val copiedArray = intent.getBooleanArrayExtra(INTENT_MESSAGE)!!
            for (i in 0 until totalCells) {
                items.add(Cell(copiedArray[i]))
                newItems.add(Cell())
            }
        } else {
            for (i in 0 until totalCells) {
                items.add(Cell())
                newItems.add(Cell())
            }
        }

        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = GridLayoutManager(this, cellsHorizontal)
        recyclerView.adapter = CellAdapter(items) { position -> onListItemClick(position) }

        generationButton = findViewById(R.id.generationButton)

        saveButton = findViewById(R.id.saveButton)
        loadButton = findViewById(R.id.loadButton)
        cloneButton = findViewById(R.id.cloneButton)

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
        backgroundColorBar = findViewById(R.id.background_color_bar)
        backgroundColorBar.setOnColorChangeListener(object: ColorSeekBar.OnColorChangeListener{
            override fun onColorChangeListener(color: Int) {
                backColor = color
                recyclerView.adapter?.notifyDataSetChanged()
            }
        })

        saveButton.setOnClickListener {
            createFile(Uri.EMPTY)
        }

        loadButton.setOnClickListener {
            openFile(Uri.EMPTY)
        }

        cloneButton.setOnClickListener {

            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra(INTENT_MESSAGE, items.map { c -> c.alive }.toBooleanArray())
            }
            startActivity(intent)
        }
    }

    // https://developer.android.com/training/data-storage/shared/documents-files
    // Request code
    val CREATE_FILE = 1
    val OPEN_FILE = 2

    private fun createFile(pickerInitialUri: Uri) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "pattern.json")

            // Optionally, specify a URI for the directory that should be opened in
            // the system file picker before your app creates the document.
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
        }
        startActivityForResult(intent, CREATE_FILE)
    }

    private fun openFile(pickerInitialUri: Uri) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"

            // Optionally, specify a URI for the file that should appear in the
            // system file picker when it loads.
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
        }

        startActivityForResult(intent, OPEN_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if(requestCode == CREATE_FILE) {
            if (data != null) {
                val gson = Gson()
                val jsonString = gson.toJson(items)

                val uri = resultData?.data!!
                val os: OutputStream = baseContext.contentResolver.openOutputStream(uri)!!

                if (os != null) {
                    os.write(jsonString.toByteArray())
                    os.close()
                }
            }
        }
        else if (requestCode == OPEN_FILE) {
            if (data != null) {
                val gson = Gson()

                val uri = resultData?.data!!
                val ins: InputStream = baseContext.contentResolver.openInputStream(uri)!!

                var result = ""
                if (ins != null) {
                    result = String(ins.readBytes())
                    ins.close()
                }

                val cellListType = object : TypeToken<List<Cell>>() {}.type
                val newData: MutableList<Cell> = gson.fromJson(result, cellListType)
                for (i in 0 until totalCells) {
                    items[i].alive = newData[i].alive
                }
                recyclerView.adapter?.notifyDataSetChanged()
            }
        }

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
                button.setBackgroundColor(backColor)
            }

            button.animate().apply {
                rotationBy(90f)
            }.start()
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