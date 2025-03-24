package com.marianhello.bgloc.provider;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Criteria;
import android.location.GnssStatus;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.marianhello.bgloc.Config;
import com.marianhello.logging.LoggerManager;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by finch on 7.11.2017.
 */

public class RawLocationProvider extends AbstractLocationProvider implements LocationListener {
    private LocationManager locationManager;
    private boolean isStarted = false;
    private Timer timer = new Timer();

    private GnssStatus.Callback gnssStatusCallback;
    private int globalInFix = 0;
    private  int globalInView = 0;


    public RawLocationProvider(Context context) {
        super(context);
        PROVIDER_ID = Config.RAW_PROVIDER;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public void onStart() {
      logger.debug("Raw start");
        if (isStarted) {
          logger.debug("Raw start is Started return");
            return;
        }
        String provider = LocationManager.GPS_PROVIDER;
        if (!locationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER) ||
                Build.VERSION.SDK_INT <= 30) {
            Criteria criteria = new Criteria();
            criteria.setAltitudeRequired(false);
            criteria.setBearingRequired(false);
            criteria.setSpeedRequired(true);
            criteria.setCostAllowed(true);
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            criteria.setHorizontalAccuracy(translateDesiredAccuracy(mConfig.getDesiredAccuracy()));
            criteria.setPowerRequirement(Criteria.POWER_HIGH);
            provider = locationManager.getBestProvider(criteria, true);
        }
        try {
            logger.info("Requesting location updates from provider {}", provider);
            final String finalProvider = provider;
            locationManager.requestLocationUpdates(finalProvider, mConfig.getInterval(), mConfig.getDistanceFilter(), this);

          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            gnssStatusCallback = new GnssStatus.Callback() {
              @Override
              public void onSatelliteStatusChanged(@NonNull GnssStatus status) {
                globalInFix = 0;
                globalInView = 0;
                int satelliteCount = status.getSatelliteCount();
                for (int i = 0; i < satelliteCount; i++) {
                  if (status.usedInFix(i)) {
                    globalInFix++;
                  } else {
                    globalInView++;
                  }
                }
              }
            };
            locationManager.registerGnssStatusCallback(gnssStatusCallback);
          }

          timer.cancel();
            timer =  new Timer();
            timer.schedule(new TimerTask() {
            @Override
                public void run() {
                    try {
                    logger.info("TT- ostatnia lokalizacja");
                    Location location = Objects.requireNonNull(locationManager.getLastKnownLocation(finalProvider));
                    onLocationChanged(location);
                    } catch (Exception e) {
                    logger.info("TT- ostatnia lokalizacja Error: " + e.getMessage());
                    }
                }
            }, 0, mConfig.getInterval());
            isStarted = true;
        } catch (SecurityException e) {
            logger.error("Security exception: {}", e.getMessage());
            this.handleSecurityException(e);
        }
    }

    @Override
    public void onStop() {
      logger.debug("Raw stop");
        if (!isStarted) {
          logger.debug("Raw is stoped");
            return;
        }
        try {
            locationManager.removeUpdates(this);
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locationManager.unregisterGnssStatusCallback(gnssStatusCallback);
          }
          timer.cancel();
        } catch (SecurityException e) {
            logger.error("Security exception: {}", e.getMessage());
            this.handleSecurityException(e);
        } finally {
            isStarted = false;
        }
    }

    @Override
    public void onConfigure(Config config) {
        super.onConfigure(config);
        if (isStarted) {
            onStop();
            onStart();
        }
    }

    @Override
    public boolean isStarted() {
        return isStarted;
    }

    @Override
    public void onLocationChanged(Location location) {
        logger.debug("Location change: {}", location.toString());
        showDebugToast("acy:" + location.getAccuracy() + ",v:" + location.getSpeed());
        int inFix = 0;
        int inView = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
          inFix = globalInFix;
          inView = globalInView;
        } else  {
          @SuppressLint("MissingPermission") GpsStatus gpsStatus = locationManager.getGpsStatus(null);
          Iterable<GpsSatellite> satellites = gpsStatus.getSatellites();
          for (GpsSatellite satellite : satellites) {
            if (satellite.usedInFix()) {
              inFix++;
            } else {
              inView++;
            }
          }
        }
        String total =  inFix + "/" + (inFix + inView);
      handleLocation(location, total);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle bundle) {
        logger.debug("Provider {} status changed: {}", provider, status);
    }

    @Override
    public void onProviderEnabled(String provider) {
        logger.debug("Provider {} was enabled", provider);
    }

    @Override
    public void onProviderDisabled(String provider) {
        logger.debug("Provider {} was disabled", provider);
    }

    /**
     * Translates a number representing desired accuracy of Geolocation system from set [0, 10, 100, 1000].
     * 0:  most aggressive, most accurate, worst battery drain
     * 1000:  least aggressive, least accurate, best for battery.
     */
    private Integer translateDesiredAccuracy(Integer accuracy) {
        if (accuracy >= 1000) {
            return Criteria.ACCURACY_LOW;
        }
        if (accuracy >= 100) {
            return Criteria.ACCURACY_MEDIUM;
        }
        if (accuracy >= 10) {
            return Criteria.ACCURACY_HIGH;
        }
        if (accuracy >= 0) {
            return Criteria.ACCURACY_HIGH;
        }

        return Criteria.ACCURACY_MEDIUM;
    }

    @Override
    public void onDestroy() {
        logger.debug("Destroying RawLocationProvider");
        this.onStop();
        super.onDestroy();
    }
}
