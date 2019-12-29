package ch.epfl.concertdrone.preprogrammed;

import android.util.Log;

import com.parrot.arsdk.arcommands.ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM;

import ch.epfl.concertdrone.drone.BebopDrone;

/**
 * This class will be called on when the "Execute Program" Button is pushed on the drone controller.
 */
public class BebopDroneRoutine {

    private BebopDrone mBebopDrone;
    private static final String TAG = "BebopDroneRoutine";
    /**
     * Create the BebopDroneRoutine object and being the pre-programmed sequence. Will only begin the sequence if
     * the drone is currently hovering.
     *
     * @param miniDrone {@link BebopDrone} object to use to send commands
     */
    public BebopDroneRoutine(BebopDrone miniDrone) {
        Log.i(TAG, "entering BebopDroneRoutine Class BebopDroneRoutine");
        mBebopDrone = miniDrone;

        // If the drone is NOT HOVERING, then don't execute.
        if (miniDrone.getFlyingState().equals(ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM
                .ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING)) {
            execute();
        }
    }

    /**
     * This method is called when the class is initialized and should contain the sequence to command the drone.
     */
    public void execute() {
        // Make your drone do things here....

        // Some quick help:

        // Go RIGHT / LEFT
        // Use "mMiniDrone.setRoll((byte) n);" where n is a number from -100 to 100 to fly the drone left or right.
        // Negative numbers are left, positive right. Use numbers closer to 0 for slower speeds, and closer to
        // +- 100 for faster speeds. 0 to stop the roll.
////////////////////////////////////////////////////////////////////////////////////////////////////
        // Simple path to go right and left a few times

        // Going right (begin)
        mBebopDrone.setRoll((byte) 10);
        mBebopDrone.setFlag((byte) 1);
        sleep(200); // maintain the action and sleep the thread for a certain amount of [ms] ("mills")
        for (int i = 0; i < 2; i++) {

            // Going Left
            mBebopDrone.setRoll((byte) -10);
            mBebopDrone.setFlag((byte) 1);
            sleep(400);

            // Going Right
            mBebopDrone.setRoll((byte) 10);
            mBebopDrone.setFlag((byte) 1);
            sleep(400);

        }
        // Going left (end)
        mBebopDrone.setRoll((byte) -10);
        sleep(200); // maintain the action and sleep the thread for a certain amount of [ms] ("mills")
        mBebopDrone.setRoll((byte) 0);
        mBebopDrone.setFlag((byte) 0);
////////////////////////////////////////////////////////////////////////////////////////////////////

        // Use "mMiniDrone.setPitch((byte) n);" where n is a number from -100 to 100 to fly the drone forward or backward.
        // Negative numbers are backward, positive forward. Use numbers closer to 0 for slower speeds, and closer to
        // +- 100 for faster speeds. 0 to stop the pitch.

        // Use "mMiniDrone.setFlag((byte) n);" where n is a 0 or 1, to tell the drone to use the pitch and/or roll values,
        // if set to 1, or to ignore them, if set to 0. In other words, this needs to be set to 1 if you want to pitch
        // or roll the drone.

        // Use "mMiniDrone.setYaw((byte) n);" where n is a number from -100 to 100 to spin the drone clockwise or
        // counterclockwise. Negative numbers are counter-clockwise, positive clockwise. Use numbers closer to 0 for
        // slower speeds, and closer to +- 100 for faster speeds. 0 to stop the spin.

        // Use "mMiniDrone.setGaz((byte) n);" where n is a number from -100 to 100 to fly the drone up or down vertically.
        // Negative numbers are down, positive up. Use numbers closer to 0 for slower speeds, and closer to
        // +- 100 for faster speeds. 0 to stop the vertical motion.

        // Keep in mind these method will return immediately... If you want your drone do maintain an action for
        // some amount of time, you will need to sleep the thread.
        // Use "sleep(mills);"
    }

    /**
     * Force the thread to sleep. Wrapper around {@link Thread}.sleep().
     *
     * @param mills
     */
    private void sleep(int mills) { // Do not edit this method
        try {
            Thread.sleep(mills);
        } catch (InterruptedException ex) {
            // Don't care....
        }
    }
}
