package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.security.data.AlarmStatus;
import com.udacity.catpoint.security.data.ArmingStatus;
import com.udacity.catpoint.security.data.SecurityRepository;
import com.udacity.catpoint.security.data.Sensor;
import com.udacity.catpoint.security.data.SensorType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {

  private SecurityService securityService;

  @Mock
  private ImageService imageService;
  @Mock
  private SecurityRepository securityRepository;
  @Mock
  private StatusListener statusListener;
  private final String randomStr = UUID.randomUUID().toString();


  @BeforeEach
  public void setup() {
    securityService = new SecurityService(securityRepository, imageService);
  }


  /**
   * Application requirement #1
   * If alarm is armed and a sensor becomes activated, put the system into pending alarm status.
   */
  @Test
  public void ifAlarmArmedAndSensorActivated_SetPendingStatus() {
    Sensor sensor = new Sensor("door", SensorType.DOOR);
    when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
    when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
    securityService.changeSensorActivationStatus(sensor, true);

    verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
  }


  /**
   * Application requirement #2
   * If alarm is armed and a sensor becomes activated and the system is already pending alarm, set the alarm status to alarm.
   */
  @Test
  public void ifAlarmArmedSensorActivatedSystemPendingAlarm_SetAlarmStatus() {
    Sensor sensor = new Sensor(randomStr, SensorType.WINDOW);
    when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
    when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
    securityService.changeSensorActivationStatus(sensor, true);

    verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
  }


  /**
   * Application requirement #3
   * If pending alarm and all sensors are inactive, return to no alarm state.
   * (Note here the change of alarm state only happens when sensor state changes to inactive
   * So, after initialisation is set to true (activated) so update to false (inactive) is a state change.)
   */
  @Test
  public void ifPendingAlarmSensorsInactive_SetNoAlarmState() {
    Sensor sensor = new Sensor(randomStr, SensorType.MOTION);
    sensor.setActive(true);
    when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
    securityService.changeSensorActivationStatus(sensor, false);

    verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
  }


  /**
   * Application requirement #4
   * If alarm is active, change in sensor state should not affect the alarm state.
   */
  @Test
  public void ifAlarmActiveChangeSensorState_NoEffectAlarmState() {
    Sensor sensor = new Sensor(randomStr, SensorType.DOOR);
    when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

    securityService.changeSensorActivationStatus(sensor, true);
    verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));

    securityService.changeSensorActivationStatus(sensor, false);
    verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
  }


  /**
   * Application requirement #5
   * If a sensor is activated while already active and the system is in pending state, change it to alarm state.
   */
  @Test
  public void ifSensorActivatedAlarmActiveSystemPending_SetAlarmState() {
    when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
    Sensor sensor = new Sensor(randomStr, SensorType.WINDOW);
    sensor.setActive(true);
    securityService.changeSensorActivationStatus(sensor, true);

    verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
  }


  /**
   * Application requirement #6
   * If a sensor is deactivated while already inactive, make no changes to the alarm state.
   */
  @Test
  public void ifSensorDeactivatedWhileInactive_NoEffectAlarmState() {
    Sensor sensor = new Sensor(randomStr, SensorType.MOTION);
    // as default setting for new sensors is inactive, this is deactivating the sonsor when already inactive
    securityService.changeSensorActivationStatus(sensor, false);

    verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));

  }


  /**
   * Application requirement #7
   * If the image service identifies an image containing a cat while the system is armed-home,
   * put the system into alarm status.
   */
  @Test
  public void ifSystemArmedCatDetected_SetAlarmStatus() {
    BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
    when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
    when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
    securityService.processImage(image);
    verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
  }


  /**
   * Application requirement #8
   * If the image service identifies an image that does not contain a cat, change the status to no alarm as long
   * as the sensors are not active.
   * 2 parts to this test.  Both using an image identified as not a cat.
   * Test 1 - a sensor is active - should be no change to alarm state
   * Test 2 - sensors inactive - alarm state should change to no alarm
   */
  @Test
  public void ifImageNotCatSensorsInactive_SetNoAlarmState() {
    BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
    when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(false);

    Set<Sensor> sensors = new HashSet<>();
    sensors.add(new Sensor("door", SensorType.DOOR));
    sensors.add(new Sensor("window", SensorType.WINDOW));
    sensors.add(new Sensor("motion", SensorType.MOTION));
    when(securityRepository.getSensors()).thenReturn(sensors);
    securityService.processImage(image);
    verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
  }


  @Test
  public void ifImageNotCatSensorActive_NoChangeAlarmState() {
    BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
    when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(false);

    Set<Sensor> sensors = new HashSet<>();
    sensors.add(new Sensor("door", SensorType.DOOR));
    sensors.add(new Sensor("window", SensorType.WINDOW));
    sensors.add(new Sensor("motion", SensorType.MOTION));
    for (Sensor sensor: sensors) {
      sensor.setActive(true);
    }
    when(securityRepository.getSensors()).thenReturn(sensors);

    securityService.processImage(image);
    verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
  }

  /**
   * Application requirement #9
   * If the system is disarmed, set the status to no alarm.
   */
  @Test
  public void ifSystemDisarmed_SetNoAlarmState() {
    securityService.setArmingStatus(ArmingStatus.DISARMED);
    verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
  }


  /**
   * Application requirement #10
   * If the system is armed, reset all sensors to inactive.
   */
  @ParameterizedTest
  @EnumSource(value = ArmingStatus.class, names={"ARMED_HOME", "ARMED_AWAY"})
  public void ifSystemArmed_ResetSensorsInactive(ArmingStatus armingStatus) {
    Set<Sensor> sensors = new HashSet<>();
    sensors.add(new Sensor("door", SensorType.DOOR));
    sensors.add(new Sensor("window", SensorType.WINDOW));
    sensors.add(new Sensor("motion", SensorType.MOTION));
    for (Sensor sensor: sensors) {
      sensor.setActive(true);
    }
    when(securityRepository.getSensors()).thenReturn(sensors);
    securityService.setArmingStatus(armingStatus);
    for (Sensor sensor: sensors) {
      assertFalse(sensor.getActive());
    }
  }


  /**
   * Application requirement #11
   * If the system is armed-home while the camera shows a cat, set the alarm status to alarm.
   */
  @Test
  public void ifCatDetectedThenSystemArmed_SetAlarmStatus() {
    BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
    when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
    when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
    securityService.processImage(image);
    verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
  }

  /** Extra tests for test coverage of SecurityService */
  @Test
  public void addGetRemoveSensors_NoException() {
    Set<Sensor> sensors = new HashSet<>();
    sensors.add(new Sensor("door", SensorType.DOOR));
    sensors.add(new Sensor("window", SensorType.WINDOW));
    sensors.add(new Sensor("motion", SensorType.MOTION));
    assertDoesNotThrow(() -> {
      for (final Sensor sensor : sensors) {
        securityService.addSensor(sensor);
        securityService.getSensors();
        securityService.removeSensor(sensor);
      }
    });
  }

  @Test
  public void addGetRemoveListeners_NoException() {
    assertDoesNotThrow(() -> {
      securityService.addStatusListener(statusListener);
      securityService.removeStatusListener(statusListener);
    });
  }

}
