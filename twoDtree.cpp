
/**
 *
 * twoDtree (pa3)
 * slight modification of a Kd tree of dimension 2.
 * twoDtree.cpp
 * This file will be used for grading.
 *
 */

#include "twoDtree.h"
#include <iostream>
#include <vector>
#include <stdio.h>
#include <math.h>



/* given */
twoDtree::Node::Node(pair<int,int> ul, pair<int,int> lr, RGBAPixel a)
	:upLeft(ul),lowRight(lr),avg(a),left(NULL),right(NULL)
	{}

/* given */
twoDtree::~twoDtree(){
	clear();
}

/* given */
twoDtree::twoDtree(const twoDtree & other) {
	copy(other);
}

/* given */
twoDtree & twoDtree::operator=(const twoDtree & rhs){
	if (this != &rhs) {
		clear();
		copy(rhs);
	}
	return *this;
}

twoDtree::twoDtree(PNG & imIn){
    /* your code here */
    width = imIn.width();
    height = imIn.height();
    pair<int,int> upLeft (0,0);
    pair<int,int> lowRight (width, height);

    stats s = stats(imIn);

    root = buildTree(s, upLeft, lowRight);

    }

twoDtree::Node * twoDtree::buildTree(stats & s, pair<int,int> ul, pair<int,int> lr) {
	Node * curr = new Node(ul, lr, s.getAvg(ul, lr));
	int topX = ul.first;
	int topY = ul.second;
    int botX = lr.first;
    int botY = lr.second;

    if (topX == botX && topY == botY){
        return curr;
        }

    curr->upLeft = ul;
    curr->lowRight = lr;
    curr->avg = s.getAvg(ul, lr);

    pair<int, int> minSplitOne (topX, botY);
    pair<int, int> minSplitTwo (topX + 1, topY);
    int minScore = s.getScore(ul, minSplitOne) + s.getScore(minSplitTwo, lr);


	for (int i = topX + 1; i < botX; i++){ //split horizontally
	    pair<int, int> splitOne (i, botY);
        pair<int, int> splitTwo (i + 1, topY);
        int currScore = s.getScore(ul, splitOne) + s.getScore(splitTwo, lr);
        if (currScore < minScore){
            minScore = currScore;
            minSplitOne.first = i;
            minSplitOne.second = botY;
            minSplitTwo.first = i + 1;
            minSplitTwo.second = topY;
            }
	    }
	for (int j = topY; j < botY; j++){ //split vertically
        pair<int, int> splitOne (botX, j);
        pair<int, int> splitTwo (topX, j + 1);
        int currScore = s.getScore(ul, splitOne) + s.getScore(splitTwo, lr);
        if (currScore < minScore){
            minScore = currScore;
            minSplitOne.first = botX;
            minSplitOne.second = j;
            minSplitTwo.first = topX;
            minSplitTwo.second = j + 1;
            }
        }

    curr->left = buildTree (s, ul, minSplitOne);
    curr->right = buildTree (s, minSplitTwo, lr);

	return curr;
}

PNG twoDtree::render(){
	/* your code here */
	PNG image = PNG(width, height);
    vector<Node *> leaves;
    leafTraversal(leaves, root);

    for (unsigned long i = 0; i <= leaves.size(); i++){
        Node* curr = leaves.at(i);
        int x = (curr->upLeft).first;
        int y = (curr->upLeft).first;
        *image.getPixel(x, y) = curr->avg;
        }
    return image;
    }


void twoDtree::leafTraversal(vector<Node*> leaves, Node* curr){
    if (curr == NULL){
        return;
        }
    if (curr->left == NULL && curr->right == NULL){
        leaves.push_back(curr);
        }
    if (curr->left != NULL){
        leafTraversal(leaves, curr->left);
        }
    if (curr->right != NULL){
        leafTraversal(leaves, curr->right);
        }
    }


void twoDtree::prune(double pct, int tol){
    if (root == NULL){
        return;
        }

    if (root->left == NULL && root->right == NULL){
        return;
        }

    pruning(pct, tol, root);
    }

void twoDtree::pruning(double pct, int tol, Node* node){
    if (node->left == NULL || node->right == NULL){
        return;
        }

    int outlier = 0;
    double percentage = 0;
    vector<Node*> leftSplit;
    vector<Node*> rightSplit;
    leafTraversal(leftSplit, node->left);
    leafTraversal(rightSplit, node->right);

    for (unsigned long i = 0; i <= leftSplit.size(); i++){
        int distance = sqrt(
            ((node->avg).r - ((leftSplit.at(i))->avg).r)^2 +
            ((node->avg).g - ((leftSplit.at(i))->avg).g)^2 +
            ((node->avg).b - ((leftSplit.at(i))->avg).b)^2);
        if (distance >= tol){ // if the difference is outside of tolerance
            outlier++;
            }
        }
    for (unsigned long j = 0; j <= rightSplit.size(); j++){
        int distance = sqrt(
            ((node->avg).r - ((leftSplit.at(j))->avg).r)^2 +
            ((node->avg).g - ((leftSplit.at(j))->avg).g)^2 +
            ((node->avg).b - ((leftSplit.at(j))->avg).b)^2);
        if (distance >= tol){ // if the difference is outside of tolerance
            outlier++;
            }
        }

    percentage = outlier /(leftSplit.size() + rightSplit.size());
    if (percentage >= pct){
        node->left = NULL;
        node->right = NULL;
        return;
        }

    pruning(pct, tol, node->left);
    pruning(pct, tol, node->right);

    }



void twoDtree::clear() {
	clearTraversal(root);
    }


void twoDtree::copy(const twoDtree & orig){
	copyTraversal(orig.root, root);
    }

void twoDtree::clearTraversal(Node* curr){
    if (curr == NULL){
        return;
        }
    if (curr->left == NULL && curr->right == NULL){
        curr = NULL;
        }

    if (curr->left != NULL){
        clearTraversal(curr->left);
        curr->left = NULL;
        }

    if (curr->right != NULL){
        clearTraversal(curr->right);
        curr->right = NULL;
        }
    }


void twoDtree::copyTraversal(Node* other, Node* curr){
    if (other == NULL){
        return;
        }

    curr->upLeft = other->upLeft;
    curr->lowRight = other->lowRight;
    curr->avg = other->avg;

    if (other->left != NULL){
        curr->left = other->left;
        copyTraversal(other->left, curr->left);
        }

    if (other->right != NULL){
        curr->right = other->right;
        copyTraversal(other->right, curr->right);
        }

    }