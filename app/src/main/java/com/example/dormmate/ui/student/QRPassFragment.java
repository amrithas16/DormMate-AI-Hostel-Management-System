package com.example.dormmate.ui.student;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.dormmate.R;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class QRPassFragment extends Fragment {

    private static final String ARG_DOC_ID = "doc_id";
    private static final String ARG_FROM = "from_date";
    private static final String ARG_TO = "to_date";

    public static QRPassFragment newInstance(String docId, String fromDate, String toDate) {
        QRPassFragment fragment = new QRPassFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DOC_ID, docId);
        args.putString(ARG_FROM, fromDate);
        args.putString(ARG_TO, toDate);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_qr_pass, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView ivQRCode = view.findViewById(R.id.ivQRCode);
        TextView tvLeaveInfo = view.findViewById(R.id.tvLeaveInfo);
        TextView tvLeaveDates = view.findViewById(R.id.tvLeaveDates);
        TextView tvBack = view.findViewById(R.id.tvQRBack);

        tvBack.setOnClickListener(v -> requireActivity().onBackPressed());

        if (getArguments() != null) {
            String docId = getArguments().getString(ARG_DOC_ID, "");
            String from = getArguments().getString(ARG_FROM, "");
            String to = getArguments().getString(ARG_TO, "");

            // QR content encodes the approval details
            String qrContent = "DormMate_GatePass|DocID:" + docId
                    + "|From:" + from + "|To:" + to + "|Status:Approved";

            try {
                BarcodeEncoder encoder = new BarcodeEncoder();
                Bitmap bitmap = encoder.encodeBitmap(qrContent, BarcodeFormat.QR_CODE, 500, 500);
                ivQRCode.setImageBitmap(bitmap);
            } catch (WriterException e) {
                Toast.makeText(getContext(), "Failed to generate QR: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }

            tvLeaveInfo.setText("✅ Leave Approved");
            tvLeaveDates.setText("From: " + from + "  →  To: " + to);
        }
    }
}
