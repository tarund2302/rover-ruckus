package com.acmerobotics.roverruckus.hardware;

import com.qualcomm.robotcore.hardware.AnalogInput;
import com.qualcomm.robotcore.hardware.DistanceSensor;
import com.qualcomm.robotcore.hardware.UltrasonicSensor;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

/**
 * This class provides API access to the MaxSonar EZ1 Ultrasonic Sensor.
 */
public class MaxSonarEZ1UltrasonicSensor implements UltrasonicSensor, DistanceSensor{
    private boolean enabled = true;
    private boolean updated = false;
    private double ultrasonicLevel = 0;

    public synchronized void update() {
        updated = false;
        if (enabled) {
            ultrasonicLevel = internalGetUltrasonicLevel();
            updated = true;
        }

    }

    public enum LogicLevel {
        V5,
        V3_3
    }

    private AnalogInput input;
    private LogicLevel logicLevel;

    public MaxSonarEZ1UltrasonicSensor(AnalogInput analogInput) {
        input = analogInput;
        logicLevel = input.getMaxVoltage() == 5 ? LogicLevel.V5 : LogicLevel.V3_3;
    }

    @Override
    public double getDistance(DistanceUnit unit) {
        synchronized (this) {
            return unit.fromInches(getUltrasonicLevel());
        }
    }

    public double getMinDistance(DistanceUnit unit) {
        return unit.fromInches(8);
    }

    private double internalGetUltrasonicLevel() {
        double voltage = input.getVoltage();
        if (logicLevel == LogicLevel.V5) {
            return voltage * 512.0 / input.getMaxVoltage();
        } else {
            return voltage * 1024.0 / input.getMaxVoltage();
        }
    }

    @Override
    public synchronized double getUltrasonicLevel() {
        return ultrasonicLevel;
    }

    @Override
    public String status() {
        return "";
    }

    @Override
    public Manufacturer getManufacturer() {
        return Manufacturer.Other;
    }

    @Override
    public String getDeviceName() {
        return "MaxSonar EZ1 Ultrasonic Sensor";
    }

    @Override
    public String getConnectionInfo() {
        return input.getConnectionInfo();
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public void resetDeviceConfigurationForOpMode() {
        input.resetDeviceConfigurationForOpMode();
    }

    @Override
    public void close() {
        input.close();
    }
}
