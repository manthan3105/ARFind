package com.example.vmac.WatBot.ar;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;

import com.beyondar.android.opengl.renderer.ARRenderer.FpsUpdatable;
import com.beyondar.android.sensor.BeyondarSensorManager;
import com.beyondar.android.util.math.geom.Ray;
import com.beyondar.android.view.OnClickBeyondarObjectListener;
import com.beyondar.android.view.OnTouchBeyondarViewListener;
import com.beyondar.android.world.BeyondarObject;
import com.beyondar.android.world.World;

/**
 * Created by Amal Krishnan on 27-03-2017.
 */

public class ArFragmentSupport extends Fragment implements FpsUpdatable,OnClickListener,
        OnTouchListener{

/**
 * Support fragment class that displays and control the
 * {@link com.beyondar.android.view.CameraView CameraView} and the
 * {@link com.beyondar.android.view.BeyondarGLSurfaceView BeyondarGLSurfaceView}
 * . It also provide a set of utilities to control the usage of the augmented
 * reality world.
 *
 */

    private static final int CORE_POOL_SIZE = 1;
    private static final int MAXIMUM_POOL_SIZE = 1;
    private static final long KEEP_ALIVE_TIME = 1000; // 1000 ms

    private ArSurfaceView mBeyondarCameraView;
    private ArBeyondarGLSurfaceView mBeyondarGLSurface;
    private TextView mFpsTextView;
    private RelativeLayout mMainLayout;

    private Camera mCamera;
    private Camera.Parameters param;

    private World mWorld;

    private OnTouchBeyondarViewListenerMod mTouchListener;
    //private OnTouchBeyondarViewListener mTouchListener;
    private OnClickBeyondarObjectListener mClickListener;

    private float mLastScreenTouchX, mLastScreenTouchY;

    private ThreadPoolExecutor mThreadPool;
    private BlockingQueue<Runnable> mBlockingQueue;

    private SensorManager mSensorManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBlockingQueue = new LinkedBlockingQueue<Runnable>();
        mThreadPool = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_TIME,
                TimeUnit.MILLISECONDS, mBlockingQueue);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mSensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
    }

    private void init() {
        ViewGroup.LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);

        mMainLayout = new RelativeLayout(getActivity());
        mBeyondarGLSurface = createBeyondarGLSurfaceView();
        mBeyondarGLSurface.setOnTouchListener(this);

        mBeyondarCameraView = createCameraView();

        mMainLayout.addView(mBeyondarCameraView, params);
        mMainLayout.addView(mBeyondarGLSurface, params);

        mBeyondarGLSurface.setMaxDistanceToRender(1000f);
        Log.d("ARFRAGG", "init: MaxDistRender"+mBeyondarGLSurface.getMaxDistanceToRender());
    }

    private void checkIfSensorsAvailable() {
        PackageManager pm = getActivity().getPackageManager();
        boolean compass = pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_COMPASS);
        boolean accelerometer = pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER);
        if (!compass && !accelerometer) {
            throw new IllegalStateException(getClass().getName()
                    + " can not run without the compass and the acelerometer sensors.");
        } else if (!compass) {
            throw new IllegalStateException(getClass().getName() + " can not run without the compass sensor.");
        } else if (!accelerometer) {
            throw new IllegalStateException(getClass().getName()
                    + " can not run without the acelerometer sensor.");
        }

    }

    /**
     * Override this method to personalize the
     * {@link com.beyondar.android.view.BeyondarGLSurfaceView
     * BeyondarGLSurfaceView} that will be instantiated.
     *
     * @return
     */
    protected ArBeyondarGLSurfaceView createBeyondarGLSurfaceView() {
        return new ArBeyondarGLSurfaceView(getActivity());
    }

    /**
     * Override this method to personalize the
     * {@link com.beyondar.android.view.CameraView CameraView} that will be
     * instantiated.
     *
     * @return
     */
    protected ArSurfaceView createCameraView() {
        mCamera=getCameraInstance();
        param=mCamera.getParameters();

        if(param.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
            param.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        mCamera.setParameters(param);

        return new ArSurfaceView(getActivity(),mCamera);
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            Log.d("Main Activity", "getCameraInstance: ERROR"+e.getMessage());
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    /**
     *
     * Returns the CameraView for this class instance.
     *
     * @return
     */
    public ArSurfaceView getCameraView() {
        return mBeyondarCameraView;
    }

    /**
     * Returns the SurfaceView for this class instance.
     *
     * @return
     */
    public com.example.vmac.WatBot.ar.ArBeyondarGLSurfaceView getGLSurfaceView() {
        return mBeyondarGLSurface;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        init();
        startRenderingAR();
        return mMainLayout;
    }

    @Override
    public void onResume() {
        super.onResume();
        mBeyondarCameraView.startPreviewCamera();
        mBeyondarGLSurface.onResume();
        BeyondarSensorManager.resume(mSensorManager);
        if (mWorld != null) {
            mWorld.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mBeyondarCameraView.releaseCamera();
        mBeyondarGLSurface.onPause();
        BeyondarSensorManager.pause(mSensorManager);
        if (mWorld != null) {
            mWorld.onPause();
        }
    }

    /**
     * Set the listener to get notified when the user touch the AR view.
     *
     * @param listener
     */
    public void setOnTouchBeyondarViewListener(OnTouchBeyondarViewListenerMod listener) {
        mTouchListener = listener;
    }

    /**
     * Set the {@link OnClickBeyondarObjectListener
     * OnClickBeyondarObjectListener} to get notified when the user click on a
     * {@link BeyondarObject BeyondarObject}
     *
     * @param listener
     */
    public void setOnClickBeyondarObjectListener(OnClickBeyondarObjectListener listener) {
        mClickListener = listener;
        mMainLayout.setClickable(listener != null);
        mMainLayout.setOnClickListener(this);
    }

    @Override
    public boolean onTouch(View v, final MotionEvent event) {
        mLastScreenTouchX = event.getX();
        mLastScreenTouchY = event.getY();

        if (mWorld == null || mTouchListener == null || event == null) {
            return false;
        }
        mTouchListener.onTouchBeyondarView(event, mBeyondarGLSurface);
        return false;
    }

    @Override
    public void onClick(View v) {
        if (v == mMainLayout) {
            if (mClickListener == null) {
                return;
            }
            final float lastX = mLastScreenTouchX;
            final float lastY = mLastScreenTouchY;

            mThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    final ArrayList<BeyondarObject> beyondarObjects = new ArrayList<BeyondarObject>();
                    mBeyondarGLSurface.getBeyondarObjectsOnScreenCoordinates(lastX, lastY, beyondarObjects);
                    if (beyondarObjects.size() == 0)
                        return;
                    mBeyondarGLSurface.post(new Runnable() {
                        @Override
                        public void run() {
                            OnClickBeyondarObjectListener listener = mClickListener;
                            if (listener != null) {
                                Log.d("ArFragment", "run: ListenerSet");
                                listener.onClickBeyondarObject(beyondarObjects);
                            }
                        }
                    });
                }
            });
        }
    }

    /**
     * Get the {@link World World} in use by the
     * fragment.
     *
     * @return
     */
    public World getWorld() {
        return mWorld;
    }

    /**
     * Set the {@link World World} that contains all
     * the {@link BeyondarObject BeyondarObject} that
     * will be displayed.
     *
     * @param world
     *            The {@link World World} that holds
     *            the information of all the elements.
     *
     * @throws IllegalStateException
     *             If the device do not have the required sensors available.
     */
    public void setWorld(World world) {
        try {
            checkIfSensorsAvailable();
        } catch (IllegalStateException e) {
            throw e;
        }
        mWorld = world;
        mBeyondarGLSurface.setWorld(world);
    }

    /**
     * Specify the delay to apply to the accelerometer and the magnetic field
     * sensor. If you don't know what is the best value, don't touch it. The
     * following values are applicable:<br>
     * <br>
     * SensorManager.SENSOR_DELAY_UI<br>
     * SensorManager.SENSOR_DELAY_NORMAL <br>
     * SensorManager.SENSOR_DELAY_GAME <br>
     * SensorManager.SENSOR_DELAY_GAME <br>
     * SensorManager.SENSOR_DELAY_FASTEST <br>
     * <br>
     *
     * @see {@link SensorManager SensorManager}
     *
     * @param delay
     *            Sensor delay.
     */
    public void setSensorDelay(int delay) {
        mBeyondarGLSurface.setSensorDelay(delay);
    }

    /**
     * Get the current sensor delay.
     *
     * @see {@link SensorManager SensorManager}
     *
     * @return Current sensor delay.
     */
    public int getSensorDelay() {
        return mBeyondarGLSurface.getSensorDelay();
    }

    /**
     * Use this method to check the frames per second.
     *
     * @param fpsUpdatable
     *            Listener that will be notified with current fps.
     *
     * @see FpsUpdatable
     */
    public void setFpsUpdatable(FpsUpdatable fpsUpdatable) {
        mBeyondarGLSurface.setFpsUpdatable(fpsUpdatable);
    }

    /**
     * Disable the GLSurface to stop rendering the AR world.
     */
    public void stopRenderingAR() {
        mBeyondarGLSurface.setVisibility(View.INVISIBLE);
    }

    /**
     * Enable the GLSurface to start rendering the AR world.
     */
    public void startRenderingAR() {
        mBeyondarGLSurface.setVisibility(View.VISIBLE);
    }

    /**
     * Get the GeoObject that intersect with the coordinates x, y on the screen.<br>
     * __Important__ When this method is called a new {@link List} is created.
     *
     * @param x
     *            X screen position.
     * @param y
     *            Y screen position.
     *
     * @return A new list with the
     *         {@link BeyondarObject BeyondarObject}
     *         that collide with the screen cord
     */
    public List<BeyondarObject> getBeyondarObjectsOnScreenCoordinates(float x, float y) {
        ArrayList<BeyondarObject> beyondarObjects = new ArrayList<BeyondarObject>();
        mBeyondarGLSurface.getBeyondarObjectsOnScreenCoordinates(x, y, beyondarObjects);
        return beyondarObjects;
    }

    /**
     * Get the GeoObject that intersect with the coordinates x, y on the screen.
     *
     * @param x
     *            X screen position.
     * @param y
     *            Y screen position.
     * @param beyondarObjects
     *            The output list where all the
     *            {@link BeyondarObject
     *            BeyondarObject} that collide with the screen cord will be
     *            stored.
     *
     */
    public void getBeyondarObjectsOnScreenCoordinates(float x, float y,
                                                      ArrayList<BeyondarObject> beyondarObjects) {
        mBeyondarGLSurface.getBeyondarObjectsOnScreenCoordinates(x, y, beyondarObjects);
    }

    /**
     * Get the GeoObject that intersect with the coordinates x, y on the screen.
     *
     * @param x
     *            screen position.
     * @param y
     *            screen position.
     * @param beyondarObjects
     *            The output list where all the
     *            {@link BeyondarObject
     *            BeyondarObject} that collide with the screen cord will be
     *            stored.
     * @param ray
     *            The ray that will hold the direction of the screen coordinate.
     *
     */
    public void getBeyondarObjectsOnScreenCoordinates(float x, float y,
                                                      ArrayList<BeyondarObject> beyondarObjects, Ray ray) {
        mBeyondarGLSurface.getBeyondarObjectsOnScreenCoordinates(x, y, beyondarObjects, ray);

    }

    /**
     * When a {@link com.beyondar.android.world.GeoObject GeoObject} is rendered
     * according to its position it could look very small if it is far away. Use
     * this method to render far objects as if there were closer.<br>
     * For instance if there are objects farther than 50 meters and we want them
     * to be displayed as they where at 50 meters, we could use this method for
     * that purpose. <br>
     * To set it to the default behavior just set it to 0
     *
     * @param maxDistanceSize
     *            The top far distance (in meters) which we want to draw a
     *            {@link com.beyondar.android.world.GeoObject GeoObject} , 0 to
     *            set again the default behavior
     */
    public void setPullCloserDistance(float maxDistanceSize) {
        mBeyondarGLSurface.setPullCloserDistance(maxDistanceSize);
    }

    /**
     * Get the distance which all the {@link com.beyondar.android.world.GeoObject
     * GeoObject} will be rendered if the are farther that the returned distance.
     *
     * @return The current max distance. 0 is the default behavior.
     */
    public float getPullCloserDistance() {
        return mBeyondarGLSurface.getPullCloserDistance();
    }

    /**
     * When a {@link com.beyondar.android.world.GeoObject GeoObject} is rendered
     * according to its position it could look very big if it is too close. Use
     * this method to render near objects as if there were farther.<br>
     * For instance if there is an object at 1 meters and we want to have
     * everything at to look like if they where at least at 10 meters, we could
     * use this method for that purpose. <br>
     * To set it to the default behavior just set it to 0.
     *
     * @param minDistanceSize
     *            The top near distance (in meters) which we want to draw a
     *            {@link com.beyondar.android.world.GeoObject GeoObject} , 0 to
     *            set again the default behavior.
     *
     */
    public void setPushAwayDistance(float minDistanceSize) {
        mBeyondarGLSurface.setPushAwayDistance(minDistanceSize);
    }

    /**
     * Get the closest distance which all the
     * {@link com.beyondar.android.world.GeoObject GeoObject} can be displayed.
     *
     * @return The current minimum distance. 0 is the default behavior.
     */
    public float getPushAwayDistance() {
        return mBeyondarGLSurface.getPushAwayDistance();
    }

    /**
     * Set the distance (in meters) which the objects will be considered to render.
     *
     * @param meters to be rendered from the user.
     */
    public void setMaxDistanceToRender(float meters) {
        mBeyondarGLSurface.setMaxDistanceToRender(meters);
    }

    /**
     * Get the distance (in meters) which the objects are being considered when
     * rendering.
     *
     * @return meters
     */
    public float getMaxDistanceToRender() {
        return mBeyondarGLSurface.getMaxDistanceToRender();
    }

    /**
     * Set the distance factor for rendering all the objects. As bigger the
     * factor the closer the objects.
     *
     */
    public void setDistanceFactor(float meters)
    {
        mBeyondarGLSurface.setDistanceFactor(meters);
    }

    /**
     * Get the distance factor.
     *
     * @return Distance factor
     */
    public float getDistanceFactor(){
        return mBeyondarGLSurface.getDistanceFactor();
    }

    /**
     * Take a screenshot of the beyondar fragment. The screenshot will contain
     * the camera and the AR world overlapped.
     *
     * @param listener
     *            {@link com.beyondar.android.screenshot.OnScreenshotListener
     *            OnScreenshotListener} That will be notified when the
     *            screenshot is ready.
     * @param options
     *            Bitmap options.
     */
//    public void takeScreenshot(OnScreenshotListener listener, BitmapFactory.Options options) {
//        ScreenshotHelper.takeScreenshot(getCameraView(), getGLSurfaceView(), listener, options);
//    }

    /**
     * Take a screenshot of the beyondar fragment. The screenshot will contain
     * the camera and the AR world overlapped.
     *
     * @param listener
     *            {@link com.beyondar.android.screenshot.OnScreenshotListener
     *            OnScreenshotListener} That will be notified when the
     *            screenshot is ready.
     */
//    public void takeScreenshot(OnScreenshotListener listener) {
//        BitmapFactory.Options options = new BitmapFactory.Options();
//        // TODO: Improve this part
//        options.inSampleSize = 4;
//        // options.inSampleSize = 1;
//        takeScreenshot(listener, options);
//    }

    /**
     * Show the number of frames per second in the left upper corner. False by
     * default.
     *
     * @param show
     *            True to show the FPS, false otherwise.
     */
    public void showFPS(boolean show) {
        if (show) {
            if (mFpsTextView == null) {
                mFpsTextView = new TextView(getActivity());
                mFpsTextView.setBackgroundResource(android.R.color.black);
                mFpsTextView.setTextColor(getResources().getColor(android.R.color.white));
                LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                mMainLayout.addView(mFpsTextView, params);
            }
            mFpsTextView.setVisibility(View.VISIBLE);
            setFpsUpdatable(this);
        } else if (mFpsTextView != null) {
            mFpsTextView.setVisibility(View.GONE);
            setFpsUpdatable(null);
        }
    }

    @Override
    public void onFpsUpdate(final float fps) {
        if (mFpsTextView != null) {
            mFpsTextView.post(new Runnable() {
                @Override
                public void run() {
                    mFpsTextView.setText("fps: " + fps);
                }
            });
        }
    }

    /**
     * Set the adapter to draw the views on top of the AR View.
     *
     * @param adapter
     */
    public void setBeyondarViewAdapter(BeyondarViewAdapter adapter) {
        mBeyondarGLSurface.setBeyondarViewAdapter(adapter, mMainLayout);
    }

    /**
     * Use this method to fill all the screen positions of the
     * {@link BeyondarObject BeyondarObject} when a
     * object is rendered. Remember that the information is filled when the
     * object is rendered, so it is asynchronous.<br>
     *
     * After this method is called you can use the following:<br>
     * {@link BeyondarObject
     * BeyondarObject.getScreenPositionBottomLeft()}<br>
     * {@link BeyondarObject
     * BeyondarObject.getScreenPositionBottomRight()}<br>
     * {@link BeyondarObject
     * BeyondarObject.getScreenPositionTopLeft()}<br>
     * {@link BeyondarObject
     * BeyondarObject.getScreenPositionTopRight()}
     *
     * __Important__ Enabling this feature will reduce the FPS, use only when is
     * needed.
     *
     * @param fill
     *            Enable or disable this feature.
     */
    public void forceFillBeyondarObjectPositionsOnRendering(boolean fill) {
        mBeyondarGLSurface.forceFillBeyondarObjectPositionsOnRendering(fill);
    }

    /**
     * Use this method to fill all the screen positions of the
     * {@link BeyondarObject BeyondarObject}. After
     * this method is called you can use the following:<br>
     * {@link BeyondarObject
     * BeyondarObject.getScreenPositionBottomLeft()}<br>
     * {@link BeyondarObject
     * BeyondarObject.getScreenPositionBottomRight()}<br>
     * {@link BeyondarObject
     * BeyondarObject.getScreenPositionTopLeft()}<br>
     * {@link BeyondarObject
     * BeyondarObject.getScreenPositionTopRight()}
     *
     * @param beyondarObject
     *            The {@link BeyondarObject
     *            BeyondarObject} to compute
     */
    public void fillBeyondarObjectPositions(BeyondarObject beyondarObject) {
        mBeyondarGLSurface.fillBeyondarObjectPositions(beyondarObject);
    }

    /**
     * Use setPullCloserDistance instead.
     */
    @Deprecated
    public void setMaxFarDistance(float maxDistanceSize) {
        setPullCloserDistance(maxDistanceSize);
    }

    /**
     * Use getPushFrontDistance instead.
     */
    @Deprecated
    public float getMaxDistanceSize() {
        return getPullCloserDistance();
    }

    /**
     * Use setPushAwayDistance instead.
     */
    @Deprecated
    public void setMinFarDistanceSize(float minDistanceSize) {
        setPushAwayDistance(minDistanceSize);
    }

    /**
     * Use getPushAwayDistance instead.
     */
    @Deprecated
    public float getMinDistanceSize() {
        return getPushAwayDistance();
    }
}

