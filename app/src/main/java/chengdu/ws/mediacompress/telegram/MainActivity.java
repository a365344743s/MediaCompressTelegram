package chengdu.ws.mediacompress.telegram;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.blankj.utilcode.constant.PermissionConstants;
import com.blankj.utilcode.util.PermissionUtils;

import org.telegram.messenger.MediaController;
import org.telegram.messenger.VideoEditedInfo;

public class MainActivity extends AppCompatActivity {
    private TextView infoTV;
    private Button telegramBTN;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_activity_main);
        infoTV = findViewById(R.id.tv_info);
        telegramBTN = findViewById(R.id.btn_telegram);

        PermissionUtils.permissionGroup(PermissionConstants.STORAGE)
                .request();

        String videoPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/source.mp4";
        String attachPathTelegram = this.getFilesDir() + "/convert/telegram.mp4";
        telegramBTN.setOnClickListener(view -> {
            Integer telegramId = org.telegram.messenger.VideoConvertUtil.startVideoConvert(videoPath, attachPathTelegram, new MediaController.ConvertorListener() {
                @Override
                public void onConvertStart(VideoEditedInfo info, float progress, long lastFrameTimestamp) {

                }

                @Override
                public void onConvertProgress(VideoEditedInfo info, long availableSize, float progress, long lastFrameTimestamp) {
                    infoTV.setText("Telegram progress: " + progress);
                }

                @Override
                public void onConvertSuccess(VideoEditedInfo info, long fileLength, long lastFrameTimestamp) {
                    infoTV.setText("Telegram success: " + info.toString());
                }

                @Override
                public void onConvertFailed(VideoEditedInfo info, float progress, long lastFrameTimestamp) {
                    infoTV.setText("Telegram failed: " + info.toString());

                }
            });
            if (telegramId == null) {
                infoTV.setText("Telegram start failed:");
            }
        });
    }
}
