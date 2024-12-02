package org.example.寻找等级;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class FindLevelDemo {
    public static void main(String[] args) {
        List<LevelNode> list = new ArrayList<>();
        LevelNode levelNode1 = new LevelNode();
//        LevelNode levelNode1_1 = new LevelNode();
//        levelNode1.child.add(levelNode1_1);
//        LevelNode levelNode1_1_1 = new LevelNode();
//        levelNode1_1.child.add(levelNode1_1_1);

        LevelNode levelNode2 = new LevelNode();
        LevelNode levelNode2_1 = new LevelNode();
        levelNode2.child.add(levelNode2_1);
        LevelNode levelNode2_2 = new LevelNode();
        levelNode2.child.add(levelNode2_2);
        LevelNode levelNode2_3 = new LevelNode();
        levelNode2.child.add(levelNode2_3);
        LevelNode levelNode2_3_1 = new LevelNode();
        levelNode2_3.child.add(levelNode2_3_1);
        levelNode2_3.child.add(levelNode2_3_1);

        LevelNode levelNode2_3_2 = new LevelNode();
        levelNode2_3.child.add(levelNode2_3_2);
        LevelNode levelNode3 = new LevelNode();
        list.add(levelNode1);
        list.add(levelNode2);
        list.add(levelNode3);
        find(list);
    }

    public static void find(List<LevelNode> list){
        List<LevelNode> result = new ArrayList<>();
        Deque<LevelNode> deque = new LinkedList<>();
        for (int i = 0; i < list.size(); i++) {
            LevelNode otherInfo3 = list.get(i);
//            int level = 1;
            otherInfo3.setLevel(1);
            otherInfo3.setNo(String.valueOf(1));
            // 遍历一级
            deque.push(otherInfo3);
            // 准备进行迭代遍历
            while (!deque.isEmpty()){
                // 对当前层进行遍历
                int dequeSize = deque.size();
                for (int dequeIndex = 0; dequeIndex < dequeSize; dequeIndex++) {
                    LevelNode parentItem = deque.poll();
                    assert parentItem != null;
                    int level = parentItem.getLevel();
                    String no = parentItem.getNo() == null ? String.valueOf(i+1) : parentItem.getNo();
//                    parentItem.setLevel(level);
                    if (level == 1) {
                        judgeJoin(result,parentItem,no,level);
                            List<LevelNode> childList = parentItem.getChild();
                            pushChild(childList,parentItem,deque,level);

                    } else {
                        judgeJoin(result,parentItem,no,level);
                        List<LevelNode> childList = parentItem.getChild();
                        pushChild(childList,parentItem,deque,level);
                    }
                }
            }
        }
        for (LevelNode levelNode : result) {
            System.out.println(levelNode);
        }

    }

    public static void judgeJoin(List<LevelNode> result,LevelNode parentItem,String no,Integer level){
        if (result.isEmpty() || !result.contains(parentItem)){
//            parentItem.setLevel(level);
//            parentItem.setNo((parentItem.getNo() == null ? String.valueOf(i+1) : parentItem.getNo()) + "-" + );
            result.add(parentItem);
        }
    }

    public static void pushChild(List<LevelNode> childList,LevelNode parentItem,Deque<LevelNode> deque,Integer parentLevel){
        childList = childList.stream().distinct().collect(Collectors.toList());
        if (!deque.isEmpty()){
            // 如果有值，证明可能是上一级
            for (int i1 = childList.size()-1; i1 > 0; i1--) {
                LevelNode child = childList.get(i1);
//                child.setNo(parentItem.getNo() + "-" + (i1 + 1));
                child.setLevel(parentLevel+1);
                deque.push(child);
            }
        }else {
            for (int i1 = 0; i1 < childList.size(); i1++) {
                LevelNode child = childList.get(i1);
//                child.setNo(parentItem.getNo() + "-" + (i1 + 1));
                child.setLevel(parentLevel+1);
                deque.add(child);
            }
        }
    }

    @Getter
    @Setter
    public static class LevelNode{
        private Integer level;
        private String no;
        private List<LevelNode> child;
        public LevelNode(){
            this.child = new ArrayList<>();
        }

        @Override
        public String toString() {
            return "LevelNode{" +
                    "level=" + level +
                    ", no='" + no + '\'' +
                    ", child=" + child +
                    '}';
        }
    }
}
