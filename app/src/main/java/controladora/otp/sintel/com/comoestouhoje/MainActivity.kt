package controladora.otp.sintel.com.comoestouhoje

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import android.app.ProgressDialog
import android.content.Intent
import android.provider.MediaStore
import android.graphics.Bitmap
import android.widget.ImageView
import com.microsoft.projectoxford.face.FaceServiceRestClient
import android.os.AsyncTask
import com.microsoft.projectoxford.face.contract.Face
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.microsoft.projectoxford.face.FaceServiceClient.FaceAttributeType


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class MainActivity : AppCompatActivity() {

    private val PICK_IMAGE = 1
    private val mHideHandler = Handler()
    private val mHidePart2Runnable = Runnable {
        // Delayed removal of status and navigation bar

        // Note that some of these constants are new as of API 16 (Jelly Bean)
        // and API 19 (KitKat). It is safe to use them, as they are inlined
        // at compile-time and do nothing on earlier devices.

    }

    private val mShowPart2Runnable = Runnable {
        // Delayed display of UI elements
        supportActionBar?.show()

    }

    private var mVisible: Boolean = false
    private val mHideRunnable = Runnable { hide() }
    private val faceServiceClient = FaceServiceRestClient("https://brazilsouth.api.cognitive.microsoft.com/face/v1.0", "4c64e149602341a480c43b4e49b28ea9")

    private val CAMERA_REQUEST = 1888
    private var imageViewFoto: ImageView? = null

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private val mDelayHideTouchListener = View.OnTouchListener { _, _ ->

        if (AUTO_HIDE) {
            delayedHide(AUTO_HIDE_DELAY_MILLIS)
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mVisible = true

        button1.setOnClickListener(View.OnClickListener {
            val gallIntent = Intent(Intent.ACTION_GET_CONTENT)
            gallIntent.type = "image/*"
            startActivityForResult(Intent.createChooser(gallIntent, "Select Picture"), PICK_IMAGE)
        })

        // FOTO
        //this.imageViewFoto = R.id.imageViewFoto as ImageView

//        buttonFoto.setOnClickListener {
//            val cameraIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
//            startActivityForResult(cameraIntent, CAMERA_REQUEST)
//        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null && data.data != null) {

            var uri = data.data

            var bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
            var imageView = imageView1

            imageView.setImageBitmap(bitmap)

            // This is the new addition.
            detectAndFrame(bitmap)
        }

        if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {

            var photo = data!!.extras.get("data")  as Bitmap
            imageViewFoto!!.setImageBitmap(photo)

            detectAndFrame(photo)
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100)
    }

// Detect faces by uploading face images
    // Frame faces after detection

    private fun detectAndFrame(imageBitmap: Bitmap?) {

        val outputStream = ByteArrayOutputStream()
        imageBitmap?.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        val inputStream = ByteArrayInputStream(outputStream.toByteArray())

        val faceAtt = FaceAttributeType.Emotion

        val returnFacesAttributes = arrayOf(faceAtt)

        val detectTask = @SuppressLint("StaticFieldLeak")
        object : AsyncTask<InputStream, String, Array<Face>>() {

            override fun doInBackground(vararg params: InputStream): Array<Face>? {

                try {

                    publishProgress("Detecting...")

                    val result = faceServiceClient.detect(
                            params[0],
                            true, // returnFaceId
                            false, returnFacesAttributes// returnFaceAttributes: a string like "age, gender"
                    )// returnFaceLandmarks

                    if (result == null) {

                        publishProgress("Detection Finished. Nothing detected")
                        return null
                    }

                    publishProgress(String.format("Detection Finished. %d face(s) detected",result!!.size))

                    return result
                }
                catch (e: Exception) {

                    publishProgress("Detection failed")

                    e.printStackTrace()
                    return null
                }
            }

            override fun onPreExecute() {
                //detectionProgressDialog!!.show()
            }

            override fun onProgressUpdate(vararg progress: String) {
                //detectionProgressDialog!!.setMessage(progress[0])
            }

            override fun onPostExecute(result: Array<Face>) {

                //detectionProgressDialog!!.dismiss()

                if (result == null) return

                val imageView = findViewById<View>(R.id.imageView1) as ImageView

                imageView.setImageBitmap(drawFaceRectanglesOnBitmap(imageBitmap!!, result))

                imageBitmap.recycle()
            }
        }
        detectTask.execute(inputStream)
    }

    private fun toggle() {
        if (mVisible) {
            hide()
        } else {
            show()
        }
    }

    private fun drawFaceRectanglesOnBitmap(originalBitmap: Bitmap, faces: Array<Face>?): Bitmap {

        val bitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        paint.color = Color.RED

        val stokeWidth = 2

        paint.strokeWidth = stokeWidth.toFloat()

        var strEmocao = "Emoção: "

        if (faces != null) {

            for (face in faces) {

                val faceRectangle = face.faceRectangle

                canvas.drawRect(
                        faceRectangle.left.toFloat(),
                        faceRectangle.top.toFloat(),
                        (faceRectangle.left + faceRectangle.width).toFloat(),
                        (faceRectangle.top + faceRectangle.height).toFloat(),
                        paint)

                strEmocao = "raiva: " + face.faceAttributes.emotion.anger
                strEmocao = "$strEmocao, disgosto: " + face.faceAttributes.emotion.disgust
                strEmocao = "$strEmocao, medo: " + face.faceAttributes.emotion.fear
                strEmocao = "$strEmocao, felicidade: " + face.faceAttributes.emotion.happiness
                strEmocao = "$strEmocao, tristeza: " + face.faceAttributes.emotion.sadness
                strEmocao = "$strEmocao, surpresa: " + face.faceAttributes.emotion.surprise
                strEmocao = "$strEmocao, neutro: " + face.faceAttributes.emotion.neutral

                textViewEmocao.text = strEmocao
            }
        }

        return bitmap
    }

    private fun hide() {
        // Hide UI first
        supportActionBar?.hide()

        mVisible = false

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable)
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    private fun show() {
        // Show the system bar

        mVisible = true

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable)
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    /**
     * Schedules a call to hide() in [delayMillis], canceling any
     * previously scheduled calls.
     */
    private fun delayedHide(delayMillis: Int) {
        mHideHandler.removeCallbacks(mHideRunnable)
        mHideHandler.postDelayed(mHideRunnable, delayMillis.toLong())
    }

    companion object {
        /**
         * Whether or not the system UI should be auto-hidden after
         * [AUTO_HIDE_DELAY_MILLIS] milliseconds.
         */
        private val AUTO_HIDE = true

        /**
         * If [AUTO_HIDE] is set, the number of milliseconds to wait after
         * user interaction before hiding the system UI.
         */
        private val AUTO_HIDE_DELAY_MILLIS = 3000

        /**
         * Some older devices needs a small delay between UI widget updates
         * and a change of the status and navigation bar.
         */
        private val UI_ANIMATION_DELAY = 300
    }
}
