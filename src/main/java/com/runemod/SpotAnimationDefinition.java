package com.runemod;

public class SpotAnimationDefinition {

    int id;

    int archive;

    public int sequence;

    short[] recolorFrom;

    short[] recolorTo;

    short[] retextureFrom;

    short[] retextureTo;

    int widthScale;

    int heightScale;

    int orientation;

    int ambient;

    int contrast;


    SpotAnimationDefinition() {
        this.sequence = -1; // L: 18
        this.widthScale = 128; // L: 23
        this.heightScale = 128; // L: 24
        this.orientation = 0; // L: 25
        this.ambient = 0; // L: 26
        this.contrast = 0; // L: 27
    } // L: 29


    void decode(Buffer var1) {
        while (true) {
            int var2 = var1.readUnsignedByte(); // L: 49
            if (var2 == 0) { // L: 50
                return; // L: 53
            }

            this.decodeNext(var1, var2); // L: 51
        }
    }


    void decodeNext(Buffer var1, int var2) {
        if (var2 == 1) { // L: 56
            this.archive = var1.readUnsignedShort();
            System.out.println("spotArchive: "+archive);
        } else if (var2 == 2) { // L: 57
            this.sequence = var1.readUnsignedShort();
            System.out.println("spotAnimSequence: "+sequence);
        } else if (var2 == 4) { // L: 58
            this.widthScale = var1.readUnsignedShort();
            System.out.println("spotWidthScale: "+widthScale);
        } else if (var2 == 5) { // L: 59
            this.heightScale = var1.readUnsignedShort();
            System.out.println("spotHeightScale: "+heightScale);
        } else if (var2 == 6) { // L: 60
            this.orientation = var1.readUnsignedShort();
            System.out.println("spotOrientation: "+orientation);
        } else if (var2 == 7) { // L: 61
            this.ambient = var1.readUnsignedByte();
            System.out.println("spotAmbient: "+ambient);
        } else if (var2 == 8) { // L: 62
            this.contrast = var1.readUnsignedByte();
        } else {
            int var3;
            int var4;
            if (var2 == 40) { // L: 63
                System.out.println("decoding reColors");
                var3 = var1.readUnsignedByte(); // L: 64
                this.recolorFrom = new short[var3]; // L: 65
                this.recolorTo = new short[var3]; // L: 66

                for (var4 = 0; var4 < var3; ++var4) { // L: 67
                    this.recolorFrom[var4] = (short)var1.readUnsignedShort(); // L: 68
                    this.recolorTo[var4] = (short)var1.readUnsignedShort(); // L: 69
                }
            } else if (var2 == 41) { // L: 72
                System.out.println("decoding reTextures");
                var3 = var1.readUnsignedByte(); // L: 73
                this.retextureFrom = new short[var3]; // L: 74
                this.retextureTo = new short[var3]; // L: 75

                for (var4 = 0; var4 < var3; ++var4) { // L: 76
                    this.retextureFrom[var4] = (short)var1.readUnsignedShort(); // L: 77
                    this.retextureTo[var4] = (short)var1.readUnsignedShort(); // L: 78
                }
            }
        }

    }


/*    public final Model getModel(int var1) {
        //Model var2 = (Model)SpotAnimationDefinition_cachedModels.get((long)this.id); // L: 85
        Model var2 = null; // L: 85
        if (var2 == null) { // L: 86
            ModelData var3 = ModelData.ModelData_get(SpotAnimationDefinition_modelArchive, this.archive, 0); // L: 87
            if (var3 == null) { // L: 88
                return null;
            }

            int var4;
            if (this.recolorFrom != null) { // L: 89
                for (var4 = 0; var4 < this.recolorFrom.length; ++var4) { // L: 90
                    var3.recolor(this.recolorFrom[var4], this.recolorTo[var4]); // L: 91
                }
            }

            if (this.retextureFrom != null) { // L: 94
                for (var4 = 0; var4 < this.retextureFrom.length; ++var4) { // L: 95
                    var3.retexture(this.retextureFrom[var4], this.retextureTo[var4]); // L: 96
                }
            }

            var2 = var3.toModel(this.ambient + 64, this.contrast + 850, -30, -50, -30); // L: 99
            //SpotAnimationDefinition_cachedModels.put(var2, (long)this.id); // L: 100
        }

        Model var5;
        if (this.sequence != -1 && var1 != -1) { // L: 103
            var5 = Player.SequenceDefinition_get(this.sequence).transformSpotAnimationModel(var2, var1);
        } else {
            var5 = var2.toSharedSpotAnimationModel(true); // L: 104
        }

        if (this.widthScale != 128 || this.heightScale != 128) { // L: 105
            var5.scale(this.widthScale, this.heightScale, this.widthScale);
        }

        if (this.orientation != 0) { // L: 106
            if (this.orientation == 90) { // L: 107
                var5.rotateY90Ccw();
            }

            if (this.orientation == 180) { // L: 108
                var5.rotateY90Ccw(); // L: 109
                var5.rotateY90Ccw(); // L: 110
            }

            if (this.orientation == 270) { // L: 112
                var5.rotateY90Ccw(); // L: 113
                var5.rotateY90Ccw(); // L: 114
                var5.rotateY90Ccw(); // L: 115
            }
        }

        return var5; // L: 118
    }*/
}

