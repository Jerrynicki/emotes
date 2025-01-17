package io.github.kosmx.emotes.main.emotePlay;

import io.github.kosmx.emotes.common.emote.EmoteData;
import io.github.kosmx.emotes.common.opennbs.SoundPlayer;
import io.github.kosmx.emotes.common.opennbs.format.Layer;
import io.github.kosmx.emotes.common.tools.Easing;
import io.github.kosmx.emotes.common.tools.MathHelper;
import io.github.kosmx.emotes.api.Pair;
import io.github.kosmx.emotes.common.tools.Vector3;
import io.github.kosmx.emotes.executor.emotePlayer.IEmotePlayer;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

// abstract to extend it in every environments
public abstract class EmotePlayer<T> implements IEmotePlayer {
    private final EmoteData data;
    @Nullable
    final SoundPlayer song;
    private boolean isRunning = true;
    private int currentTick = 0;
    private boolean isLoopStarted = false;

    protected float tickDelta;

    /**
     * Will be removed when I give up the 1.16- support
     */
    @Deprecated
    public final BodyPart head;
    @Deprecated
    public final BodyPart torso;
    @Deprecated
    public final BodyPart rightArm;
    @Deprecated
    public final BodyPart leftArm;
    @Deprecated
    public final BodyPart rightLeg;
    @Deprecated
    public final BodyPart leftLeg;
    public final HashMap<String, BodyPart> bodyParts;
    public int perspective = 0;

    /**
     *
     * @param emote emote to play
     * @param noteConsumer {@link Layer.Note} consumer
     * @param t begin playing from tick
     */
    @Nullable
    public EmotePlayer(EmoteData emote, Consumer<Layer.Note> noteConsumer, int t) {
        this.data = emote;
        if (emote.song != null) {
            this.song = new SoundPlayer(emote.song, noteConsumer, 0);
        }
        else {
            this.song = null;
        }

        this.bodyParts = new HashMap<>(emote.bodyParts.size());
        for(Map.Entry<String, EmoteData.StateCollection> part:emote.bodyParts.entrySet()){
            this.bodyParts.put(part.getKey(), new BodyPart(part.getValue()));
        }

        head = this.bodyParts.get(data.head.name);
        torso = this.bodyParts.get(data.body.name);
        rightArm = this.bodyParts.get(data.rightArm.name);
        leftArm = this.bodyParts.get(data.leftArm.name);
        rightLeg = this.bodyParts.get(data.rightLeg.name);
        leftLeg = this.bodyParts.get(data.leftLeg.name);

        this.currentTick = t;
        if(isInfinite() && t > data.returnToTick){
            currentTick = (t - data.returnToTick)%(data.endTick- data.returnToTick) + data.returnToTick;
        }
        this.perspective = perspective;
    }

    @Override
    public void tick() {
        if (this.isRunning) {
            this.currentTick++;
            if (data.isInfinite && this.currentTick >= data.endTick) {
                this.currentTick = data.returnToTick;
                this.isLoopStarted = true;
            }
            if (currentTick >= data.stopTick) {
                this.stop();
            }
            if (SoundPlayer.isPlayingSong(this.song)) song.tick();
        }
    }

    @Override
    public int getTick() {
        return this.currentTick;
    }

    /**
     * Is emotePlayer running
     *
     * @param emote EmotePlayer, can be null
     * @return is running
     */
    public static boolean isRunningEmote(@Nullable EmotePlayer emote) {
        return emote != null && emote.isRunning;
    }

    public void stop() {
        isRunning = false;
        //if(this.perspectiveRedux) PerspectiveReduxProxy.setPerspective(false);
        //if(this.perspective != null) MinecraftClient.getInstance().options.setPerspective(perspective); //TODO
    }

    @Override
    public boolean isRunning() {
        return this.isRunning;
    }

    /**
     * is the emote already in an infinite loop?
     *
     * @return :D
     */
    @Override
    public boolean isLoopStarted() {
        return isLoopStarted;
    }

    @Override
    public EmoteData getData() {
        return data;
    }

    public BodyPart getPart(String string){
        BodyPart part = bodyParts.get(string);
        return part == null ? new BodyPart(null) : part;
    }

    public void setTickDelta(float tickDelta) {
        this.tickDelta = tickDelta;
    }

    public int getStopTick() {
        return this.data.stopTick;
    }

    public int getCurrentTick() {
        return currentTick;
    }

    public boolean isInfinite() {
        return data.isInfinite;
    }

    protected abstract void updateBodyPart(BodyPart bodyPart, T modelPart);

    public class BodyPart {
        @Nullable
        public final EmoteData.StateCollection part;
        public final Axis x;
        public final Axis y;
        public final Axis z;
        public final RotationAxis pitch;
        public final RotationAxis yaw;
        public final RotationAxis roll;
        public final RotationAxis bendAxis;
        public final RotationAxis bend;


        public BodyPart(@Nullable EmoteData.StateCollection part) {
            this.part = part;
            if(part != null) {
                this.x = new Axis(part.x);
                this.y = new Axis(part.y);
                this.z = new Axis(part.z);
                this.pitch = new RotationAxis(part.pitch);
                this.yaw = new RotationAxis(part.yaw);
                this.roll = new RotationAxis(part.roll);
                this.bendAxis = new RotationAxis(part.bendDirection);
                this.bend = new RotationAxis(part.bend);
            }
            else {
                this.x = null;
                this.y = null;
                this.z = null;
                this.pitch = null;
                this.yaw = null;
                this.roll = null;
                this.bendAxis = null;
                this.bend = null;
            }
        }

        /**
         * public void updateBodyPart(ModelPart modelPart){
         * modelPart.pivotX = x.getValueAtCurrentTick(modelPart.pivotX);
         * modelPart.pivotY = y.getValueAtCurrentTick(modelPart.pivotY);
         * modelPart.pivotZ = z.getValueAtCurrentTick(modelPart.pivotZ);
         * modelPart.pitch = pitch.getValueAtCurrentTick(modelPart.pitch);
         * modelPart.yaw = yaw.getValueAtCurrentTick(modelPart.yaw);
         * modelPart.roll = roll.getValueAtCurrentTick(modelPart.roll);
         * @param modelPart modelPart...
         * }
         */
        public void updateBodyPart(T modelPart){
            if(part != null) EmotePlayer.this.updateBodyPart(this, modelPart);
        }

        public Pair<Float, Float> getBend() {
            if(bend == null) return new Pair<>(0f, 0f);
            return new Pair<>(this.bendAxis.getValueAtCurrentTick(0), this.bend.getValueAtCurrentTick(0));
        }

        public Vector3<Double> getBodyOffset() {
            if(this.part == null) return new Vector3<>(0d, 0d, 0d);
            double x = this.x.getValueAtCurrentTick(0);
            double y = this.y.getValueAtCurrentTick(0);
            double z = this.z.getValueAtCurrentTick(0);
            return new Vector3<>(x, y, z);
        }

        public Vector3<Float> getBodyRotation() {
            if(this.part == null) return new Vector3<>(0f, 0f, 0f);
            return new Vector3<>(
                    this.pitch.getValueAtCurrentTick(0),
                    this.yaw.getValueAtCurrentTick(0),
                    this.roll.getValueAtCurrentTick(0)
            );
        }

    }

    public class Axis {
        protected final EmoteData.StateCollection.State keyframes;


        public Axis(EmoteData.StateCollection.State keyframes) {
            this.keyframes = keyframes;
        }

        private EmoteData.KeyFrame findBefore(int pos, float currentState) {
            if (pos == -1) {
                return (currentTick < data.beginTick || keyframes.length() != 0) ?
                        new EmoteData.KeyFrame(0, currentState) :
                        (currentTick < data.endTick) ?
                                new EmoteData.KeyFrame(data.beginTick, keyframes.defaultValue) :
                                new EmoteData.KeyFrame(data.endTick, keyframes.defaultValue);
            }
            return this.keyframes.keyFrames.get(pos);
        }

        private EmoteData.KeyFrame findAfter(int pos, float currentState) {
            if (this.keyframes.length() > pos + 1) {
                return this.keyframes.keyFrames.get(pos + 1);
            }
            if(data.isInfinite){
                return this.getLastFrame();
            }
            return currentTick >= data.endTick || this.keyframes.length() != 0 ?
                    new EmoteData.KeyFrame(data.stopTick, currentState) :
                    currentTick >= getData().beginTick ?
                            new EmoteData.KeyFrame(getData().endTick, keyframes.defaultValue) :
                            new EmoteData.KeyFrame(getData().beginTick, keyframes.defaultValue);
        }

        private EmoteData.KeyFrame getLastFrame() {
            if(keyframes.length() > 0) {
                return this.keyframes.keyFrames.get(this.keyframes.length() - 1);
            }
            else {
                return new EmoteData.KeyFrame(getData().beginTick, keyframes.defaultValue);
            }
        }

        /**
         * Get the current value of this axis.
         *
         * @param currentValue the Current value of the axis
         * @return value
         */
        public float getValueAtCurrentTick(float currentValue) {
            if(keyframes.isEnabled) {
                int pos = keyframes.findAtTick(currentTick);
                EmoteData.KeyFrame keyBefore = findBefore(pos, currentValue);
                if (isLoopStarted && keyBefore.tick < data.returnToTick) {
                    keyBefore = findBefore(keyframes.findAtTick(data.endTick), currentValue);
                }
                EmoteData.KeyFrame keyAfter = findAfter(pos, currentValue);
                if (data.isInfinite && keyAfter.tick >= data.endTick) {
                    keyAfter = findAfter(keyframes.findAtTick(data.returnToTick - 1), currentValue);
                }
                return getValueFromKeyframes(keyBefore, keyAfter);
            }
            return currentValue;
        }

        /**
         * Calculate the current value between keyframes
         *
         * @param before Keyframe before
         * @param after  Keyframe after
         * @return value
         */
        protected final float getValueFromKeyframes(EmoteData.KeyFrame before, EmoteData.KeyFrame after) {
            int tickBefore = before.tick;
            int tickAfter = after.tick;
            if (tickBefore >= tickAfter) {
                if (currentTick < tickBefore) tickBefore -= data.endTick - data.returnToTick;
                else tickAfter += data.endTick - data.returnToTick;
            }
            if (tickBefore == tickAfter) return before.value;
            float f = (currentTick + tickDelta - (float) tickBefore) / (tickAfter - tickBefore);
            return MathHelper.lerp(Easing.easingFromEnum(data.isEasingBefore ? after.ease : before.ease, f), before.value, after.value);
        }

    }

    public class RotationAxis extends Axis {

        public RotationAxis(EmoteData.StateCollection.State keyframes) {
            super(keyframes);
        }

        @Override
        public float getValueAtCurrentTick(float currentValue) {
            return MathHelper.clampToRadian(super.getValueAtCurrentTick(MathHelper.clampToRadian(currentValue)));
        }
    }
}