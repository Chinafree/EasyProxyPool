package cn.ryan.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import cn.ryan.entity.EvaluateScoringEntity;

public class StringUtils {

	public static List<EvaluateScoringEntity> sortList(List<EvaluateScoringEntity> list) {
		List<EvaluateScoringEntity> l = new ArrayList<>(new TreeSet(list));

		Comparator<EvaluateScoringEntity> comparator = new Comparator<EvaluateScoringEntity>() {
			@Override
			public int compare(EvaluateScoringEntity o1, EvaluateScoringEntity o2) {
				Integer a = o1.getScore();
				Integer b = o2.getScore();
				return (a < b) ? -1 : ((a == b) ? 0 : 1);
			}
		};
		Collections.sort(l, comparator);
		return l;
	}

	public static boolean isNullOrEmpty(Object obj) {
		return obj == null || obj.equals(null) || obj.equals("null") ? true : false;
	}

}
