package com.acmerobotics.roverruckus.trajectory;

import com.acmerobotics.roadrunner.Pose2d;
import com.acmerobotics.roadrunner.path.PathBuilder;
import com.acmerobotics.roadrunner.path.heading.ConstantInterpolator;
import com.acmerobotics.roverruckus.opMode.auto.AutoAction;
import com.acmerobotics.roverruckus.util.SuperArrayList;

public class TrajectoryBuilder {

    private Waypoint lastWaypoint;
    private SuperArrayList<Trajectory> trajectories;
    private PathBuilder currentPath;
    private boolean added = false;

    public TrajectoryBuilder(Waypoint start) {
        lastWaypoint = start;
        trajectories = new SuperArrayList<>();
        currentPath = new PathBuilder(lastWaypoint.getExit());
    }

    public TrajectoryBuilder to(Waypoint waypoint) {

        currentPath.splineTo(waypoint.getEnter(), new GoodLinearInterpolator(lastWaypoint.getHeading(), waypoint.getHeading()));
        added = true;
        lastWaypoint = waypoint;
        if (waypoint.getStop()) newPath();
        return this;

    }

    public TrajectoryBuilder turnTo(double heading) {
        PointTurnTrajectory trajectory = new PointTurnTrajectory(lastWaypoint.pos(), heading);
        lastWaypoint = new Waypoint(new Pose2d(lastWaypoint.pos().pos(), heading), lastWaypoint.getExit().getHeading());
        newPath();
        trajectories.add(trajectory);
        return this;

    }

    public TrajectoryBuilder partialTurnTo(Waypoint waypoint) {
        PathBuilder path = new PathBuilder(lastWaypoint.getExit());
        path.splineTo(waypoint.getEnter(), new ConstantInterpolator(lastWaypoint.getHeading()));
        trajectories.add(new PartialTurnSplineTrajectory(path.build(), waypoint.getHeading()));
        return this;
    }

    public TrajectoryBuilder addAction (double t, AutoAction action) {
        newPath();
        trajectories.get(-1).addAction(t, action);
        return this;
    }

    public TrajectoryBuilder addActionOnStart (AutoAction action) {
        newPath();
        trajectories.get(-1).addAction(0, action);
        return this;
    }

    public TrajectoryBuilder addActionOnCompletion (AutoAction action) {
        newPath();
        trajectories.get(-1).addActionOnCompletion(action);
        return this;
    }

    private void newPath() {
        if (added) {
            trajectories.add(new SplineTrajectory(currentPath.build()));
            currentPath = new PathBuilder(lastWaypoint.getExit());
        }
        added = false;
    }

    public SuperArrayList<Trajectory> build() {
        newPath();
        return trajectories;
    }

}
