// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

import static edu.wpi.first.units.MutableMeasure.mutable;
import static edu.wpi.first.units.Units.Meters;
import static edu.wpi.first.units.Units.MetersPerSecond;
import static edu.wpi.first.units.Units.Volts;

import edu.wpi.first.units.Distance;
import edu.wpi.first.units.Measure;
import edu.wpi.first.units.MutableMeasure;
import edu.wpi.first.units.Velocity;
import edu.wpi.first.units.Voltage;
import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.RobotController;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import frc.robot.Constants.DriveConstants;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import edu.wpi.first.wpilibj2.command.sysid.SysIdRoutine;
import java.util.function.DoubleSupplier;

import com.revrobotics.CANSparkMax;
import com.revrobotics.RelativeEncoder;
import com.revrobotics.CANSparkLowLevel.MotorType;

public class Drive extends SubsystemBase {
  private final CANSparkMax m_flMotor = new CANSparkMax(10, MotorType.kBrushless);
  private final CANSparkMax m_frMotor = new CANSparkMax(11, MotorType.kBrushless);
  private final CANSparkMax m_blMotor = new CANSparkMax(12, MotorType.kBrushless);
  private final CANSparkMax m_brMotor = new CANSparkMax(13, MotorType.kBrushless);




  // The robot's drive
  private final DifferentialDrive m_drive =
      new DifferentialDrive(m_flMotor::set, m_frMotor::set);

  // The left-side drive encoder
  private final RelativeEncoder flEncoder = m_flMotor.getEncoder();
  private final RelativeEncoder frEncoder = m_frMotor.getEncoder();
  private final RelativeEncoder blEncoder = m_blMotor.getEncoder();
  private final RelativeEncoder brEncoder = m_brMotor.getEncoder();



  // The right-side drive encoder
  private final Encoder m_rightEncoder =
      new Encoder(
          DriveConstants.kRightEncoderPorts[0],
          DriveConstants.kRightEncoderPorts[1],
          DriveConstants.kRightEncoderReversed);

  // Mutable holder for unit-safe voltage values, persisted to avoid reallocation.
  private final MutableMeasure<Voltage> m_appliedVoltage = mutable(Volts.of(0));
  // Mutable holder for unit-safe linear distance values, persisted to avoid reallocation.
  private final MutableMeasure<Distance> m_distance = mutable(Meters.of(0));
  // Mutable holder for unit-safe linear velocity values, persisted to avoid reallocation.
  private final MutableMeasure<Velocity<Distance>> m_velocity = mutable(MetersPerSecond.of(0));

  // Create a new SysId routine for characterizing the drive.
  private final SysIdRoutine m_sysIdRoutine =
      new SysIdRoutine(
          // Empty config defaults to 1 volt/second ramp rate and 7 volt step voltage.
          new SysIdRoutine.Config(),
          new SysIdRoutine.Mechanism(
              // Tell SysId how to plumb the driving voltage to the motors.
              (Measure<Voltage> volts) -> {
                m_flMotor.set(volts.in(Volts) / RobotController.getBatteryVoltage());
                m_frMotor.set(volts.in(Volts) / RobotController.getBatteryVoltage());
                m_blMotor.set(volts.in(Volts) / RobotController.getBatteryVoltage());
                m_brMotor.set(volts.in(Volts) / RobotController.getBatteryVoltage());

              },
              // Tell SysId how to record a frame of data for each motor on the mechanism being
              // characterized.
              log -> {
                // Record a frame for the left motors.  Since these share an encoder, we consider
                // the entire group to be one motor.

              //FL
                log.motor("drive-fl")
                    .voltage(
                        m_appliedVoltage.mut_replace(
                            m_flMotor.get() * RobotController.getBatteryVoltage(), Volts))
                    .linearPosition(m_distance.mut_replace(flEncoder.getPosition(), Meters))
                    .linearVelocity(
                        m_velocity.mut_replace(flEncoder.getVelocity(), MetersPerSecond));
              //FR
                               log.motor("drive-fr")
                    .voltage(
                        m_appliedVoltage.mut_replace(
                            m_frMotor.get() * RobotController.getBatteryVoltage(), Volts))
                    .linearPosition(m_distance.mut_replace(frEncoder.getPosition(), Meters))
                    .linearVelocity(
                        m_velocity.mut_replace(frEncoder.getVelocity(), MetersPerSecond));
              //BL
                        log.motor("drive-bl")
                    .voltage(
                        m_appliedVoltage.mut_replace(
                            m_blMotor.get() * RobotController.getBatteryVoltage(), Volts))
                    .linearPosition(m_distance.mut_replace(blEncoder.getPosition(), Meters))
                    .linearVelocity(
                        m_velocity.mut_replace(blEncoder.getVelocity(), MetersPerSecond));
              //BR
                           log.motor("drive-br")
                    .voltage(
                        m_appliedVoltage.mut_replace(
                            m_brMotor.get() * RobotController.getBatteryVoltage(), Volts))
                    .linearPosition(m_distance.mut_replace(brEncoder.getPosition(), Meters))
                    .linearVelocity(
                        m_velocity.mut_replace(brEncoder.getVelocity(), MetersPerSecond));
              },
              // Tell SysId to make generated commands require this subsystem, suffix test state in
              // WPILog with this subsystem's name ("drive")
              this));
              
  /** Creates a new Drive subsystem. */
  public Drive() {
    // Add the second motors on each side of the drivetrain
    m_blMotor.follow(m_flMotor);
    m_brMotor.follow(m_frMotor);


    // We need to invert one side of the drivetrain so that positive voltages
    // result in both sides moving forward. Depending on how your robot's
    // gearbox is constructed, you might have to invert the left side instead.
    m_frMotor.setInverted(true);
    m_brMotor.setInverted(true);
    m_blMotor.setInverted(false);
    m_flMotor.setInverted(false);


    // Sets the distance per pulse for the encoders
    flEncoder.setPositionConversionFactor(DriveConstants.KlinearDistanceConversionFactor);
    frEncoder.setPositionConversionFactor(DriveConstants.KlinearDistanceConversionFactor);
    blEncoder.setPositionConversionFactor(DriveConstants.KlinearDistanceConversionFactor);
    brEncoder.setPositionConversionFactor(DriveConstants.KlinearDistanceConversionFactor);

    flEncoder.setVelocityConversionFactor(DriveConstants.kvelocityConversionFactor);
    frEncoder.setVelocityConversionFactor(DriveConstants.kvelocityConversionFactor);
    blEncoder.setVelocityConversionFactor(DriveConstants.kvelocityConversionFactor);
    brEncoder.setVelocityConversionFactor(DriveConstants.kvelocityConversionFactor);


  }

  /**
   * Returns a command that drives the robot with arcade controls.
   *
   * @param fwd the commanded forward movement
   * @param rot the commanded rotation
   */
  public Command arcadeDriveCommand(DoubleSupplier fwd, DoubleSupplier rot) {
    // A split-stick arcade command, with forward/backward controlled by the left
    // hand, and turning controlled by the right.
    return run(() -> m_drive.arcadeDrive(fwd.getAsDouble(), rot.getAsDouble()))
        .withName("arcadeDrive");
  }

  public Command sysIdQuasistatic(SysIdRoutine.Direction direction) {
    return m_sysIdRoutine.quasistatic(direction);
  }

  public Command sysIdDynamic(SysIdRoutine.Direction direction) {
    return m_sysIdRoutine.dynamic(direction);
  }
}

