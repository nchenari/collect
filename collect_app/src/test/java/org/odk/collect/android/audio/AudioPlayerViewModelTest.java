package org.odk.collect.android.audio;

import android.media.MediaPlayer;

import androidx.lifecycle.LiveData;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.odk.collect.android.support.LiveDataTester;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.odk.collect.android.audio.AudioPlayerViewModel.ClipState.NOT_PLAYING;
import static org.odk.collect.android.audio.AudioPlayerViewModel.ClipState.PAUSED;
import static org.odk.collect.android.audio.AudioPlayerViewModel.ClipState.PLAYING;

@RunWith(RobolectricTestRunner.class)
public class AudioPlayerViewModelTest {

    private final MediaPlayer mediaPlayer = mock(MediaPlayer.class);
    private final LiveDataTester liveDataTester = new LiveDataTester();

    private AudioPlayerViewModel viewModel;

    @Before
    public void setup() {
        viewModel = new AudioPlayerViewModel(() -> mediaPlayer);
    }

    @After
    public void teardown() {
        liveDataTester.teardown();
    }

    @Test
    public void play_resetsAndPreparesAndStartsMediaPlayer() throws Exception {
        viewModel.play("clip1", "file://audio.mp3");

        InOrder inOrder = Mockito.inOrder(mediaPlayer);

        inOrder.verify(mediaPlayer).reset();
        inOrder.verify(mediaPlayer).setDataSource("file://audio.mp3");
        inOrder.verify(mediaPlayer).prepare();
        inOrder.verify(mediaPlayer).start();
    }

    @Test
    public void play_whenAlreadyingPlayingClip_startsMediaPlayer() {
        viewModel.play("clip1", "file://audio.mp3");
        viewModel.play("clip1", "file://audio.mp3");

        verify(mediaPlayer, times(1)).reset();
        verify(mediaPlayer, times(2)).start();
    }

    @Test
    public void isPlaying_whenNothingPlaying_is_NOT_PLAYING() {
        LiveData<AudioPlayerViewModel.ClipState> isPlaying = liveDataTester.activate(viewModel.isPlaying("clip1"));

        assertThat(isPlaying.getValue(), equalTo(NOT_PLAYING));
    }

    @Test
    public void isPlaying_whenClipIDPlaying_is_PLAYING() {
        LiveData<AudioPlayerViewModel.ClipState> isPlaying = liveDataTester.activate(viewModel.isPlaying("clip1"));

        viewModel.play("clip1", "file://audio.mp3");
        assertThat(isPlaying.getValue(), equalTo(PLAYING));
    }

    @Test
    public void isPlaying_whenDifferentClipIDPlaying_is_NOT_PLAYING() {
        LiveData<AudioPlayerViewModel.ClipState> isPlaying = liveDataTester.activate(viewModel.isPlaying("clip2"));

        viewModel.play("clip1", "file://other.mp3");
        assertThat(isPlaying.getValue(), equalTo(NOT_PLAYING));
    }

    @Test
    public void isPlaying_whenClipIDPlaying_thenCompleted_is_NOT_PLAYING() {
        final LiveData<AudioPlayerViewModel.ClipState> isPlaying = liveDataTester.activate(viewModel.isPlaying("clip1"));

        viewModel.play("clip1", "file://audio.mp3");

        ArgumentCaptor<MediaPlayer.OnCompletionListener> captor = ArgumentCaptor.forClass(MediaPlayer.OnCompletionListener.class);
        verify(mediaPlayer).setOnCompletionListener(captor.capture());
        captor.getValue().onCompletion(mediaPlayer);

        assertThat(isPlaying.getValue(), equalTo(NOT_PLAYING));
    }

    @Test
    public void stop_stopsMediaPlayer() {
        viewModel.stop();
        verify(mediaPlayer).stop();
    }

    @Test
    public void isPlaying_whenClipIDPlaying_thenStopped_is_NOT_PLAYING() {
        LiveData<AudioPlayerViewModel.ClipState> isPlaying = liveDataTester.activate(viewModel.isPlaying("clip1"));

        viewModel.play("clip1", "file://audio.mp3");
        viewModel.stop();

        assertThat(isPlaying.getValue(), equalTo(NOT_PLAYING));
    }

    @Test
    public void background_releasesMediaPlayer() {
        viewModel.background();
        verify(mediaPlayer).release();
    }

    @Test
    public void isPlaying_whenPlayingAndThenBackgrounding_is_NOT_PLAYING() {
        LiveData<AudioPlayerViewModel.ClipState> isPlaying = liveDataTester.activate(viewModel.isPlaying("clip1"));

        viewModel.play("clip1", "file://audio.mp3");
        viewModel.background();

        assertThat(isPlaying.getValue(), equalTo(NOT_PLAYING));
    }

    @Test
    public void play_afterBackground_createsANewMediaPlayer() {
        RecordingMockMediaPlayerFactory factory = new RecordingMockMediaPlayerFactory();
        AudioPlayerViewModel viewModel = new AudioPlayerViewModel(factory);

        viewModel.play("clip1", "file://audio.mp3");
        assertThat(factory.createdInstances.size(), equalTo(1));
        verify(factory.createdInstances.get(0)).start();

        viewModel.background();
        viewModel.play("clip1", "file://audio.mp3");
        assertThat(factory.createdInstances.size(), equalTo(2));
        verify(factory.createdInstances.get(1)).start();
    }

    @Test
    public void pause_pausesMediaPlayer() {
        viewModel.pause();
        verify(mediaPlayer).pause();
    }

    @Test
    public void isPlaying_afterPause_is_PAUSED() {
        LiveData<AudioPlayerViewModel.ClipState> isPlaying = liveDataTester.activate(viewModel.isPlaying("clip1"));

        viewModel.play("clip1", "file://audio.mp3");
        viewModel.pause();

        assertThat(isPlaying.getValue(), equalTo(PAUSED));
    }

    @Test
    public void isPlaying_afterPause_andThenPlay_is_PLAYING() {
        final LiveData<AudioPlayerViewModel.ClipState> isPlaying = liveDataTester.activate(viewModel.isPlaying("clip1"));

        viewModel.play("clip1", "file://audio.mp3");
        viewModel.pause();
        viewModel.play("clip1", "file://audio.mp3");

        assertThat(isPlaying.getValue(), equalTo(PLAYING));
    }

    @Test
    public void onCleared_releasesMediaPlayer() {
        viewModel.onCleared();
        verify(mediaPlayer).release();
    }

    private static class RecordingMockMediaPlayerFactory implements MediaPlayerFactory {

        List<MediaPlayer> createdInstances = new ArrayList<>();

        @Override
        public MediaPlayer create() {
            MediaPlayer mock = mock(MediaPlayer.class);
            createdInstances.add(mock);

            return mock;
        }
    }
}