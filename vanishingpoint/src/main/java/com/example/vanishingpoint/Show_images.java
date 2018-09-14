package com.example.vanishingpoint;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.LineSegmentDetector;

import java.io.FileNotFoundException;
import java.util.Vector;
/**
 * Created by Lucky on 2018/6/20.
 */
public class Show_images extends Activity{
    private ImageView ImgView1;
    private ImageView ImgView3;
    private ImageView ImgView4;
    private TextView tw;
    private static final String TAG = "MainActivity";
    Bitmap bitmap;
    Bitmap copyBitmap;
    private LinearLayout vp;
    private LinearLayout lbd;
    private LinearLayout back;
    private LineSegmentDetector lineSegmentDetector;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {

        @Override
        public void onManagerConnected(int status) {
            // TODO Auto-generated method stub
            switch (status){
                case BaseLoaderCallback.SUCCESS:
                    Log.i(TAG, "成功加载");
                    break;
                default:
                    super.onManagerConnected(status);
                    Log.i(TAG, "加载失败");
                    break;
            }

        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_images);
        ImgView1=(ImageView) findViewById(R.id.imageView2);
        ImgView3=(ImageView) findViewById(R.id.ImageViewVP);
        ImgView4=(ImageView) findViewById(R.id.ImageViewLBD);
        tw=(TextView)findViewById(R.id.textView2);
        vp=(LinearLayout)findViewById(R.id.llQQ);
        lbd=(LinearLayout)findViewById(R.id.llWechat) ;
        back=(LinearLayout)findViewById(R.id.llback) ;
        Intent intent2=getIntent();
        String one=intent2.getStringExtra("one");
        bitmap = BitmapFactory.decodeFile(one);
        copyBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.RGB_565);
        ImgView1.setImageBitmap(bitmap);
        //ImgView1.setRotation(90);
        //定义“消失点”按钮的动作
        vp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tw.setVisibility(View.VISIBLE);
                vanishingpoint();
            }
        });
        //定义“查看原图”按钮的动作
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImgView1.setImageBitmap(bitmap);
                tw.setVisibility(View.GONE);
            }
        });
        //定义“LBD”按钮的动作
        lbd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LBD();
            }
        });
    }

    private void vanishingpoint(){
        Mat srcImg=new Mat();
        Mat dstMat=new Mat();
        Mat thresholdImage=new Mat();
        Utils.bitmapToMat(bitmap, srcImg);//convert original bitmap to Mat, R G B.
        Imgproc.cvtColor(srcImg,thresholdImage,Imgproc.COLOR_BGR2GRAY,4);
        Imgproc.cvtColor(srcImg,dstMat,Imgproc.COLOR_BGR2RGB,4);
        Imgproc.Canny(thresholdImage,thresholdImage,30,70,3,false);
        int row=srcImg.rows();
        int col=srcImg.cols();
        Mat copyImg=srcImg.clone();
        tw.setText("图像的宽度为："+col+" Pixels"+"\n");
        tw.append("图像的高度为："+row+" Pixels"+"\n");
        Mat left_lines=new Mat(1,100,CvType.CV_32FC2);
        Vector<Point> Intersection=new Vector<Point>();
        Mat right_lines=new Mat(1,100,CvType.CV_32FC2);
        Mat lines=new Mat();
        Imgproc.HoughLines(thresholdImage, lines, 1, Math.PI / 180, 150); //这里line的type是13
        //摘自csdn.com***************************
        int i_index=0,j_index=0;
        for( int i=0;i<lines.cols();i++){
            for(int j=0 ;j<lines.rows();j++){
                double[] vec=lines.get(j,i);
                double rho = vec[0],
                        theta = vec[1];
                Point start = new Point();
                Point end = new Point();
                double a = Math.cos(theta), b = Math.sin(theta);
                double x0 = a * rho, y0 = b * rho;
                start.x = Round(x0 + 1000 * (-b));
                start.y = Round(y0 + 1000 * (a));
                end.x = Round(x0 - 1000 * (-b));
                end.y = Round(y0 - 1000 * (a));
                if (10 * Math.PI / 180 < theta && theta < 80 * Math.PI / 180) {
                    Imgproc.line(dstMat, start, end, new Scalar(0, 255, 0), 3, 16,0);
                    left_lines.put(0,i_index++,vec);
                }
                else if (100 * Math.PI / 180 < theta && theta < 170 * Math.PI / 180) {
                    Imgproc.line(dstMat, start, end, new Scalar(0, 0, 255), 3, 16,0);
                    right_lines.put(0,j_index++,vec);
                }
                else {
                    Imgproc.line(dstMat, start, end, new Scalar(255, 0, 0), 3, 16,0);

                }
            }
        } //画线条
        //tw.append("left_lines.size"+left_lines.size()+"\n");
        //tw.append("right_lines.size"+right_lines.size()+"\n");
        drawVps(left_lines,right_lines,dstMat,Intersection,i_index,j_index); //画消失点
        Utils.matToBitmap(dstMat, copyBitmap);
        ImgView1.setImageBitmap(copyBitmap);
        tw.append("消失点的个数为："+Intersection.size()+"\n");
        tw.append("消失点的坐标为："+"("+(int)Intersection.get(0).x+","+(int)Intersection.get(0).y+")");
        //tw.append("消失点的坐标为：("+Intersection.get(0).x+","+Intersection.get(0).y+")"+"\n");
    }
    private void hough_line_detect(Mat image,Mat cdst,Mat left_lines,Mat right_lines){
        Mat dst=new Mat();
        Imgproc.Canny(image, dst, 30, 70);
        Imgproc.cvtColor(dst, cdst, Imgproc.COLOR_GRAY2BGR);
        Mat lines=new Mat();
        Imgproc.HoughLines(dst, lines, 1, Math.PI / 180, 150, 0, 0,0,Math.PI);
        for(int i=0;i<lines.cols();i++) {
            double[] rho_theta = lines.get(0, i);
            float rho = (float) rho_theta[0];
            float theta = (float) rho_theta[1];
            if (10 * Math.PI / 180 < theta && theta < 80 * Math.PI / 180) {
                left_lines.put(0,i,rho_theta);
            } else if (100 * Math.PI / 180 < theta && theta < 170 * Math.PI / 180) {
                right_lines.put(0,i,rho_theta);
            }
        }
    }
    private void drawVps(Mat left,Mat right,Mat image,Vector<Point> vec,int i_index,int j_index) {
        for(int i=0;i<i_index;i++) {
            for (int j = 0; j < j_index; j++) {
                        double[] vec1=left.get(0,i);
                        double[] vec2=right.get(0,j);
                        double rho1 = vec1[0],
                                theta1 = vec1[1],
                                rho2 = vec2[0],
                                theta2 = vec2[1];
                        double denom = (Math.sin(theta1) * Math.cos(theta2) - Math.cos(theta1) * Math.sin(theta2));
                        double x = (rho2 * Math.sin(theta1) - rho1 * Math.sin(theta2)) / denom;
                        double y = (rho1 * Math.cos(theta2) - rho2 * Math.cos(theta1)) / denom;
                        Point pt=new Point(x,y);
                        vec.add(pt);
                        Imgproc.circle(image, pt, 5, new Scalar(0, 0, 0), 5);
            }
        }
    }
    int Round(double x){
        int y;
        if(x >= (int)x+0.5)
        y = (int)x++;
        else
        y = (int)x;
        return y;
    }
    private void LBD(){

    }
    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }
}
