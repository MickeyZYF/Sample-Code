

#include "cs221util/PNG.h"
#include "cs221util/RGBAPixel.h"
#include "stats.h"
#include <utility>
#include <vector>
using namespace std;
using namespace cs221util;

long stats::getSum(char channel, pair<int,int> ul, pair<int,int> lr){
    int topX = ul.first;
    int topY = ul.second;
    int botX = lr.first;
    int botY = lr.second;

    if (channel == 'r'){
        return getSumRed(topX, topY, botX, botY);
        }
    if (channel == 'g'){
        return getSumGreen(topX, topY, botX, botY);
        }

    return getSumBlue(topX, topY, botX, botY);
    }

long stats::getSumRed(int topX, int topY, int botX, int botY){
    long sum = (sumRed.at(botY)).at(botX);

    if (topX != 0 && topY == 0){
        int blank = (sumRed.at(botY)).at(0);
        sum -= blank;
        }

    if (topX == 0 && topY != 0){
        int blank = (sumRed.at(0)).at(botX);
        sum -= blank;
        }

    if (topX != 0 && topY != 0){
        int blankY = (sumRed.at(botY)).at(0);
        int blankX = (sumRed.at(0)).at(botX);
        int overlap = (sumRed.at(topY - 1)).at(topX - 1);
        sum = sum - blankX - blankY + overlap;
        }

    return sum;
    }

long stats::getSumGreen(int topX, int topY, int botX, int botY){
    long sum = (sumGreen.at(botY)).at(botX);

    if (topX != 0 && topY == 0){
        int blank = (sumGreen.at(botY)).at(0);
        sum -= blank;
        }

    if (topX == 0 && topY != 0){
        int blank = (sumGreen.at(0)).at(botX);
        sum -= blank;
        }

    if (topX != 0 && topY != 0){
        int blankY = (sumGreen.at(botY)).at(0);
        int blankX = (sumGreen.at(0)).at(botX);
        int overlap = (sumGreen.at(topY - 1)).at(topX - 1);
        sum = sum - blankX - blankY + overlap;
        }

    return sum;
    }

long stats::getSumBlue(int topX, int topY, int botX, int botY){
    long sum = (sumBlue.at(botY)).at(botX);

    if (topX != 0 && topY == 0){
        int blank = (sumBlue.at(botY)).at(0);
        sum -= blank;
        }

    if (topX == 0 && topY != 0){
        int blank = (sumBlue.at(0)).at(botX);
        sum -= blank;
        }

    if (topX != 0 && topY != 0){
        int blankY = (sumBlue.at(botY)).at(0);
        int blankX = (sumBlue.at(0)).at(botX);
        int overlap = (sumBlue.at(topY - 1)).at(topX - 1);
        sum = sum - blankX - blankY + overlap;
        }

    return sum;
    }


long stats::getSumSq(char channel, pair<int,int> ul, pair<int,int> lr){
    int topX = ul.first;
    int topY = ul.second;
    int botX = lr.first;
    int botY = lr.second;

    if (channel == 'r'){
        return getSumSqRed(topX, topY, botX, botY);
        }
    if (channel == 'g'){
        return getSumSqGreen(topX, topY, botX, botY);
        }

    return getSumSqBlue(topX, topY, botX, botY);
    }

long stats::getSumSqRed(int topX, int topY, int botX, int botY){
    long sum = (sumsqRed.at(botY)).at(botX);

    if (topX != 0 && topY == 0){
        int blank = (sumsqRed.at(botY)).at(0);
        sum -= blank;
        }

    if (topX == 0 && topY != 0){
        int blank = (sumsqRed.at(0)).at(botX);
        sum -= blank;
        }

    if (topX != 0 && topY != 0){
        int blankY = (sumsqRed.at(botY)).at(0);
        int blankX = (sumsqRed.at(0)).at(botX);
        int overlap = (sumsqRed.at(topY - 1)).at(topX - 1);
        sum = sum - blankX - blankY + overlap;
        }

    return sum;
    }

long stats::getSumSqGreen(int topX, int topY, int botX, int botY){
    long sum = (sumsqGreen.at(botY)).at(botX);

    if (topX != 0 && topY == 0){
        int blank = (sumsqGreen.at(botY)).at(0);
        sum -= blank;
        }

    if (topX == 0 && topY != 0){
        int blank = (sumsqGreen.at(0)).at(botX);
        sum -= blank;
        }

    if (topX != 0 && topY != 0){
        int blankY = (sumsqGreen.at(botY)).at(0);
        int blankX = (sumsqGreen.at(0)).at(botX);
        int overlap = (sumsqGreen.at(topY - 1)).at(topX - 1);
        sum = sum - blankX - blankY + overlap;
        }

    return sum;
    }

long stats::getSumSqBlue(int topX, int topY, int botX, int botY){
    long sum = (sumsqBlue.at(botY)).at(botX);

    if (topX != 0 && topY == 0){
        int blank = (sumsqBlue.at(botY)).at(0);
        sum -= blank;
        }

    if (topX == 0 && topY != 0){
        int blank = (sumsqBlue.at(0)).at(botX);
        sum -= blank;
        }

    if (topX != 0 && topY != 0){
        int blankY = (sumsqBlue.at(botY)).at(0);
        int blankX = (sumsqBlue.at(0)).at(botX);
        int overlap = (sumsqBlue.at(topY - 1)).at(topX - 1);
        sum = sum - blankX - blankY + overlap;
        }

    return sum;
    }

stats::stats(PNG & im){
    int width = im.width();
    int length = im.height();

    makeSum(im, width, length);
    makeSumsq(im, width, length);
    }


void stats::makeSum(PNG & im, int width, int length){
    for (int i = 0; i <= length; i++){  // entry i, j
            vector<long> red;
            vector<long> green;
            vector<long> blue;
            long r = 0;
            long g = 0;
            long b = 0;
            for (int j = 0; j <= width; j++){

                for (int k = 0; k <= i; k++){   // sum of rectangle
                    for (int q = 0; q <= j; q++){
                        RGBAPixel curr = *im.getPixel(q, k);
                        r += curr.r;
                        g += curr.g;
                        b += curr.b;
                        }
                    }

                red.push_back(r);
                green.push_back(g);
                blue.push_back(b);
                }

            sumRed.push_back(red);
            sumBlue.push_back(green);
            sumGreen.push_back(blue);
            }
    }


void stats::stats::makeSumsq(PNG & im, int width, int length){
    for (int i = 0; i <= length; i++){  // entry i, j
            vector<long> red;
            vector<long> green;
            vector<long> blue;
            long r = 0;
            long g = 0;
            long b = 0;
            for (int j = 0; j <= width; j++){

                for (int k = 0; k <= i; k++){   // sum of rectangle
                    for (int q = 0; q <= j; q++){
                        RGBAPixel curr = *im.getPixel(q, k);
                        r += (curr.r)^2;
                        g += (curr.g)^2;
                        b += (curr.b)^2;
                        }
                    }

                red.push_back(r);
                green.push_back(g);
                blue.push_back(b);
                }

            sumRed.push_back(red);
            sumBlue.push_back(green);
            sumGreen.push_back(blue);
            }

    }


long stats::getScore(pair<int,int> ul, pair<int,int> lr){
    long score = 0;
    score = getScoreRed(ul, lr) + getScoreGreen(ul, lr) + getScoreBlue(ul, lr);
    return score;
    }

long stats::getScoreRed(pair<int,int> ul, pair<int,int> lr){
    long score = 0;
    score = getSumSq('r', ul, lr) - ((getSum('r', ul, lr))^2) / rectArea(ul, lr);
    return score;
    }

long stats::getScoreGreen(pair<int,int> ul, pair<int,int> lr){
    long score = 0;
    score = getSumSq('g', ul, lr) - ((getSum('g', ul, lr))^2) / rectArea(ul, lr);
    return score;
    }

long stats::getScoreBlue(pair<int,int> ul, pair<int,int> lr){
    long score = 0;
    score = getSumSq('b', ul, lr) - ((getSum('b', ul, lr))^2) / rectArea(ul, lr);
    return score;
    }


RGBAPixel stats::getAvg(pair<int,int> ul, pair<int,int> lr){
    int rAverage = 0;
    int gAverage = 0;
    int bAverage = 0;
    int area = rectArea(ul, lr);

    rAverage = getSum('r', ul, lr) / area;
    gAverage = getSum('g', ul, lr) / area;
    bAverage = getSum('b', ul, lr) / area;

    return RGBAPixel(rAverage, gAverage, bAverage);
    }


long stats::rectArea(pair<int,int> ul, pair<int,int> lr){
    int topX = ul.first;
    int topY = ul.second;
    int botX = lr.first;
    int botY = lr.second;
    int area = (botX - topX) * (botY - topY);

    return area;
    }