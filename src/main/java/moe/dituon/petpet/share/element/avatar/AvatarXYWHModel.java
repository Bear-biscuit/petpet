package moe.dituon.petpet.share.element.avatar;

import moe.dituon.petpet.share.CropType;
import moe.dituon.petpet.share.FitType;
import moe.dituon.petpet.share.TransformOrigin;
import moe.dituon.petpet.share.element.FrameInfo;
import moe.dituon.petpet.share.position.PositionDynamicData;
import moe.dituon.petpet.share.position.PositionXYWHCollection;
import net.coobird.thumbnailator.Thumbnails;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

class AvatarXYWHModel extends AvatarModel {
    protected final PositionXYWHCollection pos;
    protected PositionDynamicData dynamicData;

    AvatarXYWHModel(
            AvatarTemplate template,
            Supplier<List<BufferedImage>> imageSupplier,
            PositionXYWHCollection pos
    ) {
        super(template, imageSupplier, false);
        this.pos = pos;
        if (pos.isDynamical()) {
            var firstImg = super.imageList.get(0);
            this.dynamicData = PositionDynamicData.fromWH(
                    firstImg.getWidth(),
                    firstImg.getHeight()
            );
        }
        buildImage();
    }

    @Override
    public void draw(Graphics2D g2d, FrameInfo info) {
        var pos = this.pos.getPosition(info.index, dynamicData);
        var avatarImage = super.imageList.get(info.index % super.imageList.size());
        var multiple = info.getMultiple();

        int x = (int) (pos[0] * multiple);
        int y = (int) (pos[1] * multiple);
        int w = (int) (pos[2] * multiple);
        int h = (int) (pos[3] * multiple);
        BufferedImage newAvatarImage = avatarImage;
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (Objects.requireNonNull(fitType) == FitType.CONTAIN) {
            int resultWidth = w;
            int resultHeight = h;
            double avatarRatio = (double) avatarImage.getWidth() / avatarImage.getHeight();
            double canvasRatio = (double) w / h;
            if (avatarRatio > canvasRatio) {
                resultHeight = (int) (w / avatarRatio);
            } else {
                resultWidth = (int) (h * avatarRatio);
            }

            int resultX = (w - resultWidth) / 2;
            int resultY = (h - resultHeight) / 2;
            x = x + resultX;
            y = y + resultY;
            w = resultWidth;
            h = resultHeight;
        }

        if (angle == 0) {
            g2d.drawImage(newAvatarImage, x, y, w, h, null);
            return;
        }

        AffineTransform old = g2d.getTransform();
        if (super.transformOrigin == TransformOrigin.CENTER) {
            g2d.rotate(Math.toRadians(angle), (double) w / 2 + x, (double) h / 2 + y);
        } else {
            g2d.rotate(Math.toRadians(angle), x, y);
        }
        g2d.drawImage(avatarImage, x, y, w, h, null);
        g2d.setTransform(old);
    }

    @Override
    public BufferedImage buildImage(int index, BufferedImage image) {
        if (super.fitType == FitType.COVER) {
            var pos = this.pos.getPosition(index, dynamicData);
            int w = pos[2];
            int h = pos[3];

            float scale = Math.max(
                    (float) w / image.getWidth(),
                    (float) h / image.getHeight()
            );
            float scaledWidth = image.getWidth() * scale;
            float scaledHeight = image.getHeight() * scale;

            float dx = (scaledWidth - w),
                    dy = (scaledHeight - h);

            int pdx = Math.round(dx / scale / 2),
                    pdy = Math.round(dy / scale / 2);

            super.cropType = CropType.PIXEL;
            super.cropPos = new int[]{
                    pdx, pdy, image.getWidth() - pdx, image.getHeight() - pdy
            };
        }
        image = super.buildImage(index, image);
        if (super.resampling) {
            int aw = 0, ah = 0, maxSize = 0;

            for (int i = 0; i < pos.size(); i++) {
                int[] p = pos.getPosition(i);
                if (p[2] > aw) aw = p[2];
                if (p[3] > ah) ah = p[3];
                maxSize = Math.max(aw, ah);
            }

            try {
                image = Thumbnails.of(image).size(maxSize, maxSize).keepAspectRatio(true).asBufferedImage();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        return image;
    }

    @Override
    public int getPosLength() {
        return pos.size();
    }
}
