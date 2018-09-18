#include "path.h"
#include <queue>
#include <stack>
using namespace std;

path::path(const PNG & im, pair<int,int> s, pair<int,int> e)
   :start(s),end(e),image(im){
    BFS();
}

void path::BFS(){
	// initialize working vectors
	vector<vector<bool>> V(image.height(), vector<bool> (image.width(),false));
	vector<vector<pair<int,int>>> P(image.height(), vector<pair<int,int>> (image.width(),end));

    /* your code here */

    queue<pair<int, int>> q;
    q.push(start);
    V.at(start.second).at(start.first) = true;

    while (!q.empty()){
        pair<int, int> curr = q.front();
        q.pop();

        vector<pair<int,int>> neighborList = neighbors(curr);
        for (unsigned long i=0; i<neighborList.size(); i++){
            if(good(V, curr, neighborList.at(i))){
                pair<int, int> goodNeighbor = neighborList.at(i);
                int gnPredX = goodNeighbor.first;
                int gnPredY = goodNeighbor.second;
                q.push(goodNeighbor);
                V.at(gnPredY).at(gnPredX) = true;
                P.at(gnPredY).at(gnPredX) = curr;
                }
            }
    }

	pathPts = assemble(P,start,end);
}

PNG path::render(){

    /* your code here */
    for (unsigned long i=0; i<pathPts.size(); i++){
        int x = (pathPts.at(i)).first;
        int y = (pathPts.at(i)).second;

        RGBAPixel r (255, 0, 0);
        *image.getPixel(x, y) = r;
    }

    return image;
}

vector<pair<int,int>> path::getPath() { return pathPts;}

int path::length() { return pathPts.size();}

bool path::good(vector<vector<bool>> & v, pair<int,int> curr, pair<int,int> next){

    /* your code here */
    int x = image.width();
    int y = image.height();
    int x1 = curr.first;
    int y1 = curr.second;
    int x2 = next.first;
    int y2 = next.second;

    if (!(0 <= x2 && x2 < x && 0 <= y2 && y2 < y)){       // Within Image
        return false;
    }

    bool visited = (v.at(y2)).at(x2);
    if (visited){       // Unvisited
        return false;
    }

    RGBAPixel p1 = *image.getPixel(x1, y1);
    RGBAPixel p2 = *image.getPixel(x2, y2);
    if (!(closeEnough(p1, p2))){        // Close in color
        return false;
    }

    return true;
}

vector<pair<int,int>> path::neighbors(pair<int,int> curr) {
	vector<pair<int,int>> n;

    /* your code here */
    int x = curr.first;
    int y = curr.second;

    pair<int, int> up;
    up.first = x;
    up.second = y + 1;

    pair<int, int> down;
    down.first = x;
    down.second = y - 1;

    pair<int, int> left;
    left.first = x - 1;
    left.second = y;

    pair<int, int> right;
    right.first = x + 1;
    right.second = y;

    n.push_back(up);
    n.push_back(down);
    n.push_back(left);
    n.push_back(right);

    return n;
}

vector<pair<int,int>> path::assemble(vector<vector<pair<int,int>>> & p, pair<int,int> s, pair<int,int> e) {

    /* hint, gold code contains the following line:
	stack<pair<int,int>> S; */

    /* your code here */
    stack<pair<int,int>> S;
    vector<pair<int,int>> path;
    int x2 = e.first;
    int y2 = e.second;

    if (p.at(y2).at(x2) == e){//
        path.push_back(s);
        return path;
        }

    S.push(e);
    pair<int, int> pred = p.at(y2).at(x2);
    while(pred != s){
        S.push(pred);
        pred = p.at(pred.second).at(pred.first);
    }
    S.push(s);
    cout << S.size() << "" <<endl;



    while (!S.empty()){
        path.push_back(S.top());
        S.pop();
    }

    return path;


}



bool path::closeEnough(RGBAPixel p1, RGBAPixel p2){
   int dist = (p1.r-p2.r)*(p1.r-p2.r) + (p1.g-p2.g)*(p1.g-p2.g) +
               (p1.b-p2.b)*(p1.b-p2.b);

   return (dist <= 80);
}
