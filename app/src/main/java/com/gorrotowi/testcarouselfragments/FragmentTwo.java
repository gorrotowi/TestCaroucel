package com.gorrotowi.testcarouselfragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.gorrotowi.testcarouselfragments.caroucel.Carousel;
import com.gorrotowi.testcarouselfragments.caroucel.CarouselAdapter;
import com.gorrotowi.testcarouselfragments.caroucel.CarouselItem;

import java.util.ArrayList;


public class FragmentTwo extends Fragment {

    private static final String TAG = FragmentTwo.class.getSimpleName();
    Carousel carousel;
    private ArrayList<CarouselItem> mImages;
    private CarouselItem lastCarouselItem;
    private ImageView statusIv;
    private int drawables[] =
            {R.drawable.telcel, R.drawable.movistar_eps, R.drawable.verizon,
                    R.drawable.telcel, R.drawable.movistar_eps, R.drawable.verizon,
                    R.drawable.telcel, R.drawable.movistar_eps, R.drawable.verizon,
                    R.drawable.telcel, R.drawable.movistar_eps, R.drawable.verizon,
                    R.drawable.telcel, R.drawable.movistar_eps, R.drawable.verizon
            };

    public FragmentTwo() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_fragment_two, container, false);


        carousel = (Carousel) rootView.findViewById(R.id.carousel);
        statusIv = (ImageView) rootView.findViewById(R.id.statusIv);

        rootView.findViewById(R.id.topleft).setOnDragListener(new DragListener());

        carousel.setOnDragCarouselListener(new CarouselAdapter.OnDragCarouselListener() {
            @Override
            public void onStarDrag(CarouselItem item) {
                lastCarouselItem = item;
            }


        });

        carousel.setOnItemClickListener(new CarouselAdapter.OnItemClickListener() {
                                            @Override
                                            public void onItemClick(CarouselAdapter<?> parent, CarouselItem item, int position, long id) {
                                                lastCarouselItem = item;
                                                Log.e(TAG, "onDrag: Action Ended");
                                            }
                                        }
        );

        mImages = new ArrayList<CarouselItem>();
        int totalItems = 7;
        for (int i = 0; i < totalItems; i++) {
            CarouselItem item;

            if (i == totalItems - 1) {
                item = new CarouselItem(getActivity(), drawables[2], CarouselItem.CLICK);
            } else {
                item = new CarouselItem(getActivity(), "http://java.sogeti.nl/JavaBlog/wp-content/uploads/2009/04/android_icon_256.png", CarouselItem.DRAG);

            }


            mImages.add(item);
        }
        Carousel.ImageAdapter adapter = new Carousel.ImageAdapter(getActivity(), mImages);
        carousel.setAdapter(adapter);
        carousel.setSelection(2, false);

        return rootView;
    }

    class DragListener implements View.OnDragListener {
        @Override
        public boolean onDrag(View v, DragEvent event) {

            switch (event.getAction()) {

                case DragEvent.ACTION_DRAG_LOCATION:

                    break;
                case DragEvent.ACTION_DRAG_STARTED:

                    break;
                case DragEvent.ACTION_DRAG_ENTERED:

                    break;
                case DragEvent.ACTION_DRAG_EXITED:

                    break;
                case DragEvent.ACTION_DROP:
                    break;
                case DragEvent.ACTION_DRAG_ENDED:
                    Log.e(TAG, "onDrag: Action Ended");
                    break;
                default:
                    break;
            }
            return true;
        }
    }

}
