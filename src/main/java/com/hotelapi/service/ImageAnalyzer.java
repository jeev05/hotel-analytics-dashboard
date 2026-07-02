package com.hotelapi.service;

import model.Hotel;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class ImageAnalyzer {

    public static void analyze(Hotel hotel) {
        try {
            if (hotel.getImageUrl() == null) return;

            BufferedImage image = ImageIO.read(URI.create(hotel.getImageUrl()).toURL());

            double sharpness = calculateSharpness(image);
            double brightness = getBrightness(image);

            boolean isBlurry = sharpness < 300;

            double score = 0;

            if (sharpness > 500) score += 50;
            else if (sharpness > 300) score += 30;
            else score += 10;

            if (brightness > 80 && brightness < 200) score += 50;
            else score += 20;

            hotel.setImageScore(score);
            hotel.setBlurry(isBlurry);

        } catch (Exception e) {
            System.out.println("Image analysis failed");
        }
    }

    private static double calculateSharpness(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();

        double sum = 0, sumSq = 0;
        int count = 0;

        for (int x = 1; x < width - 1; x++) {
            for (int y = 1; y < height - 1; y++) {
                int gray = img.getRGB(x, y) & 0xff;
                sum += gray;
                sumSq += gray * gray;
                count++;
            }
        }

        double mean = sum / count;
        return (sumSq / count) - (mean * mean);
    }

    private static double getBrightness(BufferedImage img) {
        double total = 0;
        int count = 0;

        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;

                total += (r + g + b) / 3.0;
                count++;
            }
        }
        return total / count;
    }
   public static void analyzeMultiple(List<String> images, Hotel hotel) {
    try {
        if (images == null || images.isEmpty()) {
            System.out.println("No images found");
            return;
        }

        int count = Math.min(5, images.size());

        double totalSharpness = 0;
        double totalBrightness = 0;

        List<String> badImages = new ArrayList<>();

        for (int i = 0; i < count; i++) {

            String imageUrl = images.get(i);
            BufferedImage img = ImageIO.read(URI.create(imageUrl).toURL());

            double sharpness = calculateSharpness(img);
            double brightness = getBrightness(img);

            totalSharpness += sharpness;
            totalBrightness += brightness;

            // 🔴 Detect bad images
            if (sharpness < 800 || brightness < 80 || brightness > 200) {
                badImages.add(imageUrl);
            }
        }

        double avgSharpness = totalSharpness / count;
        double avgBrightness = totalBrightness / count;

        // 🔥 NORMALIZED SCORING

        double sharpScore;
        if (avgSharpness > 3000) sharpScore = 100;
        else if (avgSharpness > 1500) sharpScore = 80;
        else if (avgSharpness > 800) sharpScore = 60;
        else if (avgSharpness > 300) sharpScore = 40;
        else sharpScore = 20;

        double brightScore;
        if (avgBrightness >= 100 && avgBrightness <= 180) brightScore = 100;
        else if (avgBrightness >= 80 && avgBrightness <= 200) brightScore = 80;
        else if (avgBrightness >= 60 && avgBrightness <= 220) brightScore = 60;
        else brightScore = 40;

        double finalScore = (sharpScore * 0.6) + (brightScore * 0.4);

// 🔥 penalty for bad images
int badCount = badImages.size();

if (badCount >= 3) {
    finalScore -= 25;
} else if (badCount == 2) {
    finalScore -= 15;
} else if (badCount == 1) {
    finalScore -= 8;
}

// keep within bounds
finalScore = Math.max(0, Math.min(100, finalScore));
        // 🎯 OUTPUT
        System.out.println("\nIMAGE ANALYSIS");
        System.out.println("--------------");
        System.out.println("Total Images: " + images.size());
        System.out.println("Blur Score: " + (int) avgSharpness);
        System.out.println("Brightness Score: " + (int) avgBrightness);
        System.out.println("Final Image Score: " + finalScore);

        // 🔥 RECOMMENDATIONS
        System.out.println("\nRECOMMENDATIONS:");

        if (avgSharpness < 800) {
            System.out.println("- Images are blurry → Use high resolution images");
        } else if (avgSharpness < 1500) {
            System.out.println("- Image sharpness is average → Slight improvement needed");
        }

        if (avgBrightness < 80) {
            System.out.println("- Images are too dark → Improve lighting");
        } else if (avgBrightness > 200) {
            System.out.println("- Images are too bright → Reduce exposure");
        }

        if (finalScore >= 85) {
            System.out.println("- Overall image quality is EXCELLENT ✅");
        } else if (finalScore >= 70) {
            System.out.println("- Image quality is GOOD 👍");
        } else if (finalScore >= 50) {
            System.out.println("- Image quality is AVERAGE ⚠ Improve");
        } else {
            System.out.println("- Image quality is POOR ❌ Improve urgently");
        }

        // 🔴 BAD IMAGE LINKS
        System.out.println("\nBAD IMAGES (Needs Improvement):");

        if (badImages.isEmpty()) {
            System.out.println("None 🎉 All images are good");
        } else {
            for (String bad : badImages) {
                System.out.println(bad);
            }
        }

        // Set the calculated score on the hotel object
        hotel.setImageScore(finalScore);
        hotel.setBlurry(avgSharpness < 800); // Mark as blurry if sharpness is low

    } catch (Exception e) {
        System.out.println("Image analysis failed");
        // Set default values if analysis fails
        hotel.setImageScore(0.0);
        hotel.setBlurry(true);
    }
}
}